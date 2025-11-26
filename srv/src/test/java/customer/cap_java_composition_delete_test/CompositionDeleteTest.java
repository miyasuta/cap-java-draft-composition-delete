package customer.cap_java_composition_delete_test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class CompositionDeleteTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDeleteCompositionChild() throws Exception {
        System.out.println("========== TEST: Delete Composition Child in Draft Mode ==========");

        // 1. 新しいドラフトヘッダーを作成 (POST)
        String createDraftPayload = """
            {
                "title": "Test Header",
                "description": "Test Description"
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/odata/v4/composition/Headers")
                .contentType("application/json")
                .content(createDraftPayload))
            .andExpect(status().isCreated())
            .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        JsonNode headerNode = objectMapper.readTree(createResponse);
        String headerId = headerNode.get("ID").asText();

        System.out.println("Created draft header with ID: " + headerId);

        // 2. ドラフトヘッダーに明細を追加 (PATCH with items)
        String addItemsPayload = String.format("""
            {
                "items": [
                    {
                        "itemNo": 10,
                        "productName": "Product 1",
                        "quantity": 5
                    },
                    {
                        "itemNo": 20,
                        "productName": "Product 2",
                        "quantity": 10
                    }
                ]
            }
            """);

        mockMvc.perform(patch("/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=false)")
                .contentType("application/json")
                .content(addItemsPayload))
            .andExpect(status().isOk());

        System.out.println("Added 2 items to draft header");

        // 3. 明細の一覧を取得してIDを確認
        MvcResult getItemsResult = mockMvc.perform(get("/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=false)/items"))
            .andExpect(status().isOk())
            .andReturn();

        String itemsResponse = getItemsResult.getResponse().getContentAsString();
        JsonNode itemsNode = objectMapper.readTree(itemsResponse);
        JsonNode itemsArray = itemsNode.get("value");

        String item1Id = itemsArray.get(0).get("ID").asText();
        String item2Id = itemsArray.get(1).get("ID").asText();

        System.out.println("Item 1 ID: " + item1Id);
        System.out.println("Item 2 ID: " + item2Id);

        // 4. 明細2を削除 (DELETE)
        System.out.println("========== DELETING ITEM 2 ==========");

        mockMvc.perform(delete("/odata/v4/composition/Items(ID=" + item2Id + ",IsActiveEntity=false)"))
            .andExpect(status().isNoContent());

        System.out.println("Item 2 deleted via DELETE request");

        // 5. 明細が削除されたことを確認
        MvcResult verifyResult = mockMvc.perform(get("/odata/v4/composition/Headers(ID=" + headerId + ",IsActiveEntity=false)/items"))
            .andExpect(status().isOk())
            .andReturn();

        String verifyResponse = verifyResult.getResponse().getContentAsString();
        JsonNode verifyNode = objectMapper.readTree(verifyResponse);
        int remainingCount = verifyNode.get("value").size();

        System.out.println("Remaining items count: " + remainingCount);

        System.out.println("========== TEST COMPLETED ==========");
    }
}
