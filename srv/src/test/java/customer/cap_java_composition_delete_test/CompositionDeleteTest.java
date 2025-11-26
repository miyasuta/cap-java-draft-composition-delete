package customer.cap_java_composition_delete_test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CompositionDeleteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testDeleteCompositionChild() throws Exception {
        System.out.println("========================================");
        System.out.println("Test: Delete Composition Child in Edit Draft Mode");
        System.out.println("========================================");

        // 1. Create draft header with items (deep insert)
        System.out.println("\n1. Creating draft header with items...");
        String createHeaderPayload = "{"
            + "\"title\": \"Test Header\","
            + "\"description\": \"Test Description\","
            + "\"items\": ["
            + "  {\"itemNo\": 10, \"productName\": \"Product A\", \"quantity\": 5, \"price\": 100.00},"
            + "  {\"itemNo\": 20, \"productName\": \"Product B\", \"quantity\": 3, \"price\": 200.00}"
            + "]"
            + "}";

        MvcResult createResult = mockMvc.perform(post("/odata/v4/odata/v4/composition/Headers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHeaderPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        JsonNode headerNode = objectMapper.readTree(createResponse);
        String headerId = headerNode.get("ID").asText();
        System.out.println("Created draft header with ID: " + headerId + " with 2 items");

        // 2. Activate the draft (make it active)
        System.out.println("\n2. Activating draft...");
        mockMvc.perform(post("/odata/v4/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=false)/CompositionService.draftActivate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
        System.out.println("Draft activated successfully");

        // 3. Create edit draft (enter edit mode)
        System.out.println("\n3. Creating edit draft (entering edit mode)...");
        mockMvc.perform(post("/odata/v4/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=true)/CompositionService.draftEdit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"PreserveChanges\": true}"))
                .andExpect(status().isOk());
        System.out.println("Edit draft created successfully");

        // 4. Get items from the draft to get item IDs
        System.out.println("\n4. Getting items from draft...");
        MvcResult getItemsResult = mockMvc.perform(
                get("/odata/v4/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=false)/items"))
                .andExpect(status().isOk())
                .andReturn();

        String itemsResponse = getItemsResult.getResponse().getContentAsString();
        JsonNode itemsNode = objectMapper.readTree(itemsResponse);
        JsonNode itemsArray = itemsNode.get("value");

        if (itemsArray != null && itemsArray.size() > 0) {
            String firstItemId = itemsArray.get(0).get("ID").asText();
            System.out.println("Found " + itemsArray.size() + " items in draft. First item ID: " + firstItemId);

            // 5. Delete the first item from draft - THIS IS WHERE WE WANT TO SEE WHICH EVENT IS TRIGGERED
            System.out.println("\n5. ========== DELETING COMPOSITION CHILD ITEM FROM DRAFT ==========");
            System.out.println("Watch the logs to see which event handler is triggered!");

            mockMvc.perform(delete("/odata/v4/odata/v4/composition/Items(ID=" + firstItemId + ",IsActiveEntity=false)"))
                    .andExpect(status().isNoContent());

            System.out.println("Delete request completed");
            System.out.println("===================================================================\n");

            // 6. Verify deletion in draft
            System.out.println("6. Verifying logical deletion in draft...");
            MvcResult verifyDraftResult = mockMvc.perform(
                    get("/odata/v4/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=false)/items"))
                    .andExpect(status().isOk())
                    .andReturn();

            String verifyDraftResponse = verifyDraftResult.getResponse().getContentAsString();
            JsonNode verifyDraftNode = objectMapper.readTree(verifyDraftResponse);
            JsonNode verifyDraftArray = verifyDraftNode.get("value");
            System.out.println("Draft still keeps " + verifyDraftArray.size() + " items (logical delete keeps the record).");

            boolean deletedItemFlaggedInDraft = false;
            for (JsonNode node : verifyDraftArray) {
                if (node.get("ID").asText().equals(firstItemId)) {
                    deletedItemFlaggedInDraft = node.hasNonNull("isDeleted") && node.get("isDeleted").asBoolean();
                    System.out.println("Deleted draft item marked with isDeleted=" + deletedItemFlaggedInDraft);
                    break;
                }
            }
            assertTrue(deletedItemFlaggedInDraft, "Deleted draft item must remain with isDeleted=true");

            // 7. Activate the draft again (save changes)
            System.out.println("\n7. ========== ACTIVATING DRAFT (SAVING CHANGES) ==========");
            System.out.println("Watch the logs to see if any event handler is triggered during activation!");

            mockMvc.perform(post("/odata/v4/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=false)/CompositionService.draftActivate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk());

            System.out.println("Draft activation completed");
            System.out.println("===========================================================\n");

            // 8. Verify final state in active entity
            System.out.println("8. Verifying final state in active entity...");
            MvcResult verifyActiveResult = mockMvc.perform(
                    get("/odata/v4/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=true)/items"))
                    .andExpect(status().isOk())
                    .andReturn();

            String verifyActiveResponse = verifyActiveResult.getResponse().getContentAsString();
            JsonNode verifyActiveNode = objectMapper.readTree(verifyActiveResponse);
            JsonNode verifyActiveArray = verifyActiveNode.get("value");
            System.out.println("Final items in active entity: " + verifyActiveArray.size());

            boolean deletedItemFlaggedInActive = false;
            for (JsonNode node : verifyActiveArray) {
                if (node.get("ID").asText().equals(firstItemId)) {
                    deletedItemFlaggedInActive = node.hasNonNull("isDeleted") && node.get("isDeleted").asBoolean();
                    System.out.println("Deleted active item marked with isDeleted=" + deletedItemFlaggedInActive);
                    break;
                }
            }
            assertTrue(deletedItemFlaggedInActive, "Deleted active item must remain with isDeleted=true");
        } else {
            System.out.println("No items found!");
        }

        System.out.println("\n========================================");
        System.out.println("Test completed. Check the logs above to see which events were triggered.");
        System.out.println("========================================");
    }

    @Test
    public void testDeleteCompositionChildViaHeaderNavigation() throws Exception {
        System.out.println("========================================");
        System.out.println("Test: Delete Composition Child via Header Navigation");
        System.out.println("========================================");

        String createHeaderPayload = "{"
            + "\"title\": \"Nav Delete Header\","
            + "\"description\": \"Nav Delete Description\","
            + "\"items\": ["
            + "  {\"itemNo\": 30, \"productName\": \"Product NAV A\", \"quantity\": 2, \"price\": 50.00},"
            + "  {\"itemNo\": 40, \"productName\": \"Product NAV B\", \"quantity\": 4, \"price\": 75.50}"
            + "]"
            + "}";

        MvcResult createResult = mockMvc.perform(post("/odata/v4/odata/v4/composition/Headers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHeaderPayload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdHeader = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String headerId = createdHeader.get("ID").asText();
        System.out.println("Created header for navigation test with ID: " + headerId);

        mockMvc.perform(post("/odata/v4/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=false)/CompositionService.draftActivate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/odata/v4/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=true)/CompositionService.draftEdit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"PreserveChanges\": true}"))
                .andExpect(status().isOk());

        MvcResult itemsResult = mockMvc.perform(
                get("/odata/v4/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=false)/items"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode itemsArray = objectMapper.readTree(itemsResult.getResponse().getContentAsString()).get("value");
        assertTrue(itemsArray != null && itemsArray.size() > 0, "Draft items must exist before deletion");

        String firstItemId = itemsArray.get(0).get("ID").asText();
        System.out.println("Deleting draft item via navigation path. Item ID: " + firstItemId);

        String navigationDeletePath = "/odata/v4/odata/v4/composition/Headers(ID=" + headerId
                + ",IsActiveEntity=false)/items(ID=" + firstItemId + ",IsActiveEntity=false)";

        mockMvc.perform(delete(navigationDeletePath))
                .andExpect(status().isNoContent());

        MvcResult navGetResult = mockMvc.perform(get(navigationDeletePath))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode navItemNode = objectMapper.readTree(navGetResult.getResponse().getContentAsString());
        boolean flaggedAsDeleted = navItemNode.hasNonNull("isDeleted") && navItemNode.get("isDeleted").asBoolean();
        System.out.println("Navigation deleted item marked isDeleted=" + flaggedAsDeleted);
        assertTrue(flaggedAsDeleted, "Navigation delete must keep the draft row with isDeleted=true");
    }
}
