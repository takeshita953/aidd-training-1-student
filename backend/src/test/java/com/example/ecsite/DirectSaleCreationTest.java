package com.example.ecsite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 購入確定を経ずに売上を直接登録できないことを確かめる。 */
class DirectSaleCreationTest extends EcTestBase {
    @Autowired private JdbcTemplate jdbc;

    @Test
    void 認証なしで在庫もカートも無い商品の任意の売上を登録できない() throws Exception {
        long customer = data.createCustomer("direct-sale@example.com");
        long product = data.createProduct("商品", 100);
        data.createInventory(product, 0, 0);

        var response = mvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": %d,
                                  "productId": %d,
                                  "unitPrice": 987654,
                                  "quantity": 999,
                                  "totalAmount": 1,
                                  "saleDate": "2001-01-01T00:00:00Z"
                                }
                                """.formatted(customer, product)))
                .andReturn()
                .getResponse();

        assertAll(
                () -> assertFalse(response.getStatus() >= 200 && response.getStatus() < 300,
                        "Unauthenticated direct sale request must be rejected"),
                () -> assertThat(jdbc.queryForObject("SELECT count(*) FROM sale", Integer.class)).isZero(),
                () -> assertThat(data.getReservedQuantity(product)).isZero(),
                () -> assertThat(cartService.getCartItems(customer)).isEmpty()
        );
    }
}
