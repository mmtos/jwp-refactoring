package kitchenpos.table.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import kitchenpos.ServiceTest;
import kitchenpos.menu.fixture.MenuProductFixtureFactory;
import kitchenpos.order.fixture.OrderLineItemFixtureFactory;
import kitchenpos.menu.domain.MenuGroup;
import kitchenpos.menu.domain.MenuProduct;
import kitchenpos.menu.dto.MenuDto;
import kitchenpos.menu.util.MenuApplicationBehavior;
import kitchenpos.order.domain.OrderStatus;
import kitchenpos.order.dto.OrderLineItemRequest;
import kitchenpos.order.dto.OrderResponse;
import kitchenpos.order.util.OrderApplicationBehavior;
import kitchenpos.product.domain.Product;
import kitchenpos.product.util.ProductApplicationBehavior;
import kitchenpos.table.dto.OrderTableResponse;
import kitchenpos.table.exception.CannotChangeEmptyState;
import kitchenpos.table.exception.CannotChangeNumberOfGuests;
import kitchenpos.table.exception.NotExistTableException;
import kitchenpos.table.util.TableApplicationBehavior;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TableServiceTest extends ServiceTest {
    @Autowired
    TableApplicationBehavior tableApplicationBehavior;

    @Autowired
    private MenuApplicationBehavior menuApplicationBehavior;

    @Autowired
    private ProductApplicationBehavior productApplicationBehavior;

    @Autowired
    private OrderApplicationBehavior orderApplicationBehavior;

    @Autowired
    TableService tableService;

    @Test
    @DisplayName("빈 테이블 생성")
    void 빈_테이블_생성() {
        OrderTableResponse savedOrderTable = tableApplicationBehavior.빈테이블_생성됨();
        Assertions.assertAll("빈 테이블인지 확인한다."
                , () -> assertThat(savedOrderTable.getTableGroupId()).isNull()
                , () -> assertThat(savedOrderTable.getId()).isNotNull()
                , () -> assertThat(savedOrderTable.getNumberOfGuests()).isZero()
        );
    }

    @Test
    @DisplayName("비어있지 않은 테이블 생성")
    void 비어있지않은_테이블_생성() {
        int numberOfGuests = 4;
        OrderTableResponse savedOrderTable = tableApplicationBehavior.비어있지않은테이블_생성됨(numberOfGuests);
        Assertions.assertAll("비어있지 않은 테이블인지 확인한다."
                , () -> assertThat(savedOrderTable.getTableGroupId()).isNull()
                , () -> assertThat(savedOrderTable.getId()).isNotNull()
                , () -> assertThat(savedOrderTable.getNumberOfGuests()).isEqualTo(numberOfGuests)
        );
    }

    @Test
    @DisplayName("테이블 목록 조회")
    void 테이블_목록_조회() {
        tableApplicationBehavior.빈테이블_생성됨();
        tableApplicationBehavior.빈테이블_생성됨();
        List<OrderTableResponse> orderTables = tableService.list();
        assertThat(orderTables).hasSize(2);
    }

    @Test
    @DisplayName("빈 테이블로 변경")
    void 빈_테이블로_변경() {
        int numberOfGuests = 4;
        OrderTableResponse orderTable = tableApplicationBehavior.비어있지않은테이블_생성됨(numberOfGuests);
        OrderTableResponse updatedOrderTable = tableApplicationBehavior.빈테이블로_변경(orderTable.getId());
        Assertions.assertAll("빈 테이블로 변경되었는지 확인한다."
                , () -> assertThat(updatedOrderTable.getId()).isEqualTo(orderTable.getId())
                , () -> assertThat(updatedOrderTable.isEmpty()).isTrue()
        );
    }

    @Test
    @DisplayName("비어있지않은 테이블로 변경")
    void 비어있지않은_테이블로_변경() {
        OrderTableResponse orderTable = tableApplicationBehavior.빈테이블_생성됨();
        OrderTableResponse updatedOrderTable = tableApplicationBehavior.비어있지않은테이블로_변경(orderTable.getId());
        Assertions.assertAll("비어있지 않은 테이블로 변경되었는지 확인한다."
                , () -> assertThat(updatedOrderTable.getId()).isEqualTo(orderTable.getId())
                , () -> assertThat(updatedOrderTable.isEmpty()).isFalse()
                , () -> assertThat(updatedOrderTable.getNumberOfGuests()).isZero()
        );
    }

    @Test
    @DisplayName("존재하지 않는 테이블의 경우 공석여부 변경 시도시 실패")
    void 테이블_공석상태변경_테이블이_존재하지않는경우() {
        Long notExistTableId = -1L;
        assertThatThrownBy(() -> tableApplicationBehavior.비어있지않은테이블로_변경(notExistTableId))
                .isInstanceOf(NotExistTableException.class);
    }

    @Test
    @DisplayName("테이블그룹에 포함된 테이블의 경우 공석여부 변경 시도시 실패")
    void 테이블_공석상태변경_테이블그룹에_포함된경우() {
        OrderTableResponse emptyTable = tableApplicationBehavior.빈테이블_생성됨();
        OrderTableResponse emptyTable2 = tableApplicationBehavior.빈테이블_생성됨();
        tableApplicationBehavior.테이블그룹_지정됨(emptyTable, emptyTable2);

        Long tableId = emptyTable.getId();
        assertThatThrownBy(() -> tableApplicationBehavior.비어있지않은테이블로_변경(tableId))
                .isInstanceOf(CannotChangeEmptyState.class);
    }

    @Test
    @DisplayName("주문의 상태가 계산완료가 아닌 경우 공석여부 변경 시도시 실패")
    void 테이블_공석상태변경_주문이_조리_식사상태인경우() {
        OrderTableResponse table = tableApplicationBehavior.비어있지않은테이블_생성됨(3);
        OrderResponse order = 테이블에_임시_주문_추가(table.getId());
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COOKING.name());

        Long tableId = table.getId();
        assertThatThrownBy(() -> tableApplicationBehavior.빈테이블로_변경(tableId))
                .isInstanceOf(CannotChangeEmptyState.class);
    }

    @Test
    @DisplayName("테이블 인원수 변경")
    void 테이블_인원수_변경() {
        OrderTableResponse savedOrderTable = tableApplicationBehavior.비어있지않은테이블_생성됨(4);
        int updatedNumberOfGuests = 3;
        OrderTableResponse updatedOrderTable = tableApplicationBehavior.테이블_인원수_변경(savedOrderTable.getId(),
                updatedNumberOfGuests);
        Assertions.assertAll("테이블 인원수 변경여부를 확인한다."
                , () -> assertThat(updatedOrderTable.getId()).isEqualTo(savedOrderTable.getId())
                , () -> assertThat(updatedOrderTable.getNumberOfGuests()).isEqualTo(updatedNumberOfGuests)
        );
    }

    @Test
    @DisplayName("인원수가 음수일때 테이블 인원수 변경 실패")
    void 테이블_인원수_변경_음수로_변경시도() {
        OrderTableResponse savedOrderTable = tableApplicationBehavior.비어있지않은테이블_생성됨(4);

        int invalidNumberOfGuests = -5;

        Long tableId = savedOrderTable.getId();
        assertThatThrownBy(() -> tableApplicationBehavior.테이블_인원수_변경(tableId, invalidNumberOfGuests))
                .isInstanceOf(CannotChangeNumberOfGuests.class);
    }

    @Test
    @DisplayName("빈 테이블인 경우 테이블 인원수 변경 실패")
    void 테이블_인원수_변경_빈테이블인_경우() {
        OrderTableResponse savedOrderTable = tableApplicationBehavior.빈테이블_생성됨();
        int updatedNumberOfGuests = 4;

        Long tableId = savedOrderTable.getId();
        assertThatThrownBy(() -> tableApplicationBehavior.테이블_인원수_변경(tableId, updatedNumberOfGuests))
                .isInstanceOf(CannotChangeNumberOfGuests.class);
    }

    private OrderResponse 테이블에_임시_주문_추가(Long tableId) {
        MenuGroup menuGroup = menuApplicationBehavior.메뉴그룹_생성됨("메뉴그룹1");
        Product product1 = productApplicationBehavior.상품_생성됨("상품1", 1000);
        MenuProduct menuProduct = MenuProductFixtureFactory.createMenuProduct(product1.getId(), 4);
        MenuDto menu = menuApplicationBehavior.메뉴_생성됨(menuGroup, "메뉴1", 4000, Lists.newArrayList(menuProduct));
        OrderLineItemRequest orderLineItem = OrderLineItemFixtureFactory.createOrderLine(menu.getId(), 3);
        return orderApplicationBehavior.주문_생성됨(tableId, Lists.newArrayList(orderLineItem));
    }
}
