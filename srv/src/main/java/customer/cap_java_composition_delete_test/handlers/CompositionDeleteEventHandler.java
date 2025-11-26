package customer.cap_java_composition_delete_test.handlers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cds.ResultBuilder;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.draft.DraftCancelEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.compositionservice.Headers_;
import cds.gen.compositionservice.Items;
import cds.gen.compositionservice.Items_;

@Component
@ServiceName("CompositionService")
public class CompositionDeleteEventHandler implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(CompositionDeleteEventHandler.class);

    // ドラフトモードでの子アイテム削除時は物理削除せずisDeletedフラグを立てる
    @On(event = DraftService.EVENT_DRAFT_CANCEL, entity = Items_.CDS_NAME)
    public void onDraftCancelItem(DraftCancelEventContext context) {
        logger.info("====== EVENT_DRAFT_CANCEL triggered for Items (logical delete) ======");

        var analyzer = CqnAnalyzer.create(context.getModel());
        var analysisResult = analyzer.analyze(context.getCqn());

        Map<String, Object> targetKeys = analysisResult.targetKeys();
        String itemId = targetKeys != null ? (String) targetKeys.get(Items.ID) : null;

        if (itemId == null) {
            Map<String, Object> rootKeys = analysisResult.rootKeys();
            itemId = rootKeys != null ? (String) rootKeys.get(Items.ID) : null;
        }

        if (itemId == null) {
            logger.warn("Draft cancel request did not contain an Item ID – skipping logical delete.");
            return;
        }

        Items draftItem = Items.create();
        draftItem.setId(itemId);
        draftItem.setIsDeleted(Boolean.TRUE);
        final String itemIdForPatch = itemId;

        DraftService draftService = (DraftService) context.getService();
        draftService.patchDraft(Update.entity(Items_.class)
                .data(draftItem)
                .where(item -> item.ID().eq(itemIdForPatch).and(item.IsActiveEntity().eq(false))));

        context.setResult(ResultBuilder.deletedRows(1).result());
        // 以降の標準DELETE処理を止めて論理削除を確定する
        context.setCompleted();
    }

    // 親ヘッダーのUPDATE時にコンポジション削除が反映される
    @Before(event = CqnService.EVENT_UPDATE, entity = Headers_.CDS_NAME)
    public void beforeUpdateHeader() {
        logger.info("====== EVENT_UPDATE triggered for Headers (might include item deletion) ======");
    }
}
