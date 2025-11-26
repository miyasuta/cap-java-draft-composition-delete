package customer.cap_java_composition_delete_test.handlers;

import org.springframework.stereotype.Component;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cds.gen.compositionservice.Items_;
import cds.gen.compositionservice.Headers_;

@Component
@ServiceName("CompositionService")
public class ItemEventHandler implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ItemEventHandler.class);

    // 候補1: 一般的なDELETEイベント (Items)
    @Before(event = CqnService.EVENT_DELETE, entity = Items_.CDS_NAME)
    public void beforeDeleteItem() {
        logger.info("====== EVENT_DELETE triggered for Items ======");
    }

    // 候補2: ドラフトPATCHイベント (Items)
    @Before(event = DraftService.EVENT_DRAFT_PATCH, entity = Items_.CDS_NAME)
    public void beforeDraftPatchItem() {
        logger.info("====== EVENT_DRAFT_PATCH triggered for Items ======");
    }

    // 候補3: ドラフトCANCELイベント (Items)
    @Before(event = DraftService.EVENT_DRAFT_CANCEL, entity = Items_.CDS_NAME)
    public void beforeDraftCancelItem() {
        logger.info("====== EVENT_DRAFT_CANCEL triggered for Items ======");
    }

    // 候補4: UPDATEイベント (Items)
    @Before(event = CqnService.EVENT_UPDATE, entity = Items_.CDS_NAME)
    public void beforeUpdateItem() {
        logger.info("====== EVENT_UPDATE triggered for Items ======");
    }

    // 候補5: CREATEイベント (Items)
    @Before(event = CqnService.EVENT_CREATE, entity = Items_.CDS_NAME)
    public void beforeCreateItem() {
        logger.info("====== EVENT_CREATE triggered for Items ======");
    }

    // 候補6: UPDATEイベント (Headers) - コンポジションの削除は親のUPDATEとして処理される可能性
    @Before(event = CqnService.EVENT_UPDATE, entity = Headers_.CDS_NAME)
    public void beforeUpdateHeader() {
        logger.info("====== EVENT_UPDATE triggered for Headers (might include item deletion) ======");
    }

    // 候補7: ドラフトPATCHイベント (Headers)
    @Before(event = DraftService.EVENT_DRAFT_PATCH, entity = Headers_.CDS_NAME)
    public void beforeDraftPatchHeader() {
        logger.info("====== EVENT_DRAFT_PATCH triggered for Headers (might include item deletion) ======");
    }
}
