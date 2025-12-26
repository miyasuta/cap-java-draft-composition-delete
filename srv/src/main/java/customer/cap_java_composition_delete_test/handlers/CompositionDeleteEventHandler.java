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
import com.sap.cds.services.draft.DraftSaveEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.compositionservice.Headers;
import cds.gen.compositionservice.Headers_;
import cds.gen.compositionservice.Items;
import cds.gen.compositionservice.Items_;

@Component
@ServiceName("CompositionService")
public class CompositionDeleteEventHandler implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(CompositionDeleteEventHandler.class);

    // When deleting child items in draft mode, set the isDeleted flag instead of physical deletion
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
        // Stop the subsequent standard DELETE processing and finalize the logical deletion
        context.setCompleted();
    }

    // @Before handler - entity data is not passed for DRAFT_SAVE event
    // @Before(event = DraftService.EVENT_DRAFT_SAVE, entity = Headers_.CDS_NAME)
    // public void onDraftSaveHeaderBefore(DraftSaveEventContext context, Headers draftHeader) {
    //     logger.info("====== @Before EVENT_DRAFT_SAVE triggered for Headers ======");
    //     // draftHeader is null - entity data is not available in DRAFT_SAVE Before handler
    // }

    // @After handler - draft data is already deleted at this point
    // @After(event = DraftService.EVENT_DRAFT_SAVE, entity = Headers_.CDS_NAME)
    // public void onDraftSaveHeaderAfter(DraftSaveEventContext context) {
    //     logger.info("====== @After EVENT_DRAFT_SAVE triggered for Headers ======");
    //     // Draft items are already deleted (0 items found)
    // }

    // @On handler - intercept DRAFT_SAVE to read draft data before standard processing
    @On(event = DraftService.EVENT_DRAFT_SAVE, entity = Headers_.CDS_NAME)
    public void onDraftSaveHeaderOn(DraftSaveEventContext context) {
        logger.info("====== @On EVENT_DRAFT_SAVE triggered for Headers ======");

        var analyzer = CqnAnalyzer.create(context.getModel());
        var analysisResult = analyzer.analyze(context.getCqn());

        // Get the Header ID
        Map<String, Object> targetKeys = analysisResult.targetKeys();
        if (targetKeys != null) {
            String headerId = (String) targetKeys.get(Headers.ID);
            if (headerId != null) {
                logger.info("Header ID: {}", headerId);
                DraftService draftService = (DraftService) context.getService();

                try {
                    // Read draft items BEFORE standard DRAFT_SAVE processing
                    var draftItems = draftService.run(
                        com.sap.cds.ql.Select.from(Items_.class)
                            .where(item -> item.header_ID().eq(headerId)
                                .and(item.IsActiveEntity().eq(false)))
                    ).listOf(Items.class);

                    logger.info("Found {} draft items for header {} in @On handler", draftItems.size(), headerId);

                    // Store items with isDeleted=true for later processing
                    java.util.List<String> deletedItemIds = new java.util.ArrayList<>();
                    for (Items draftItem : draftItems) {
                        logger.info("Draft Item - ID: {}, productName: {}, isDeleted: {}",
                            draftItem.getId(), draftItem.getProductName(), draftItem.getIsDeleted());

                        if (Boolean.TRUE.equals(draftItem.getIsDeleted())) {
                            deletedItemIds.add(draftItem.getId());
                            logger.info("Marked item for isDeleted copy: {}", draftItem.getId());
                        }
                    }

                    // Call the default DRAFT_SAVE handler to activate the draft
                    context.setResult(draftService.run(context.getCqn()));

                    // After standard processing, copy isDeleted flag to active entities
                    if (!deletedItemIds.isEmpty()) {
                        logger.info("Copying isDeleted flag for {} items", deletedItemIds.size());

                        CqnService persistenceService = context.getService();
                        for (String itemId : deletedItemIds) {
                            logger.info("Copying isDeleted=true to active entity for item: {}", itemId);

                            Items activeItemUpdate = Items.create();
                            activeItemUpdate.setIsDeleted(Boolean.TRUE);

                            persistenceService.run(
                                com.sap.cds.ql.Update.entity(Items_.class)
                                    .data(activeItemUpdate)
                                    .where(item -> item.ID().eq(itemId)
                                        .and(item.IsActiveEntity().eq(true)))
                            );

                            logger.info("Successfully copied isDeleted=true to active entity: {}", itemId);
                        }
                    }

                    // Mark the event as completed to prevent default handler from running again
                    context.setCompleted();

                } catch (Exception e) {
                    logger.error("Error in @On DRAFT_SAVE handler: {}", e.getMessage(), e);
                }
            }
        }

        logger.info("====== @On EVENT_DRAFT_SAVE for Headers completed ======");
    }
}
