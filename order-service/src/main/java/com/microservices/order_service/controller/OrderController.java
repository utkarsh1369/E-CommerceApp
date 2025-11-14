package com.microservices.order_service.controller;

import com.microservices.order_service.model.dto.DeliveryDto;
import com.microservices.order_service.model.dto.OrderRequestDto;
import com.microservices.order_service.model.dto.OrderResponseDto;
import com.microservices.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order APIs",description = "CRUD Operations on Orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PreAuthorize("hasAuthority('USER')")
    @PostMapping
    @Operation(summary = "Create an Order",description = "Only USER can place an Order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Order Created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponseDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400",description = "Bad Request",content = @Content),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content)
    })
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto orderRequestDto) {
        OrderResponseDto createdOrder = orderService.createOrder(orderRequestDto);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @PreAuthorize("@orderSecurityService.isOrderOwner(#orderId)")
    @PutMapping("/{orderId}")
    @Operation(summary = "Update the Order",description = "Only the User itself can update the order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Order Updated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponseDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400",description = "Bad Request",content = @Content),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content),
            @ApiResponse(responseCode = "404",description = "Not Found",content = @Content)
    })
    public ResponseEntity<OrderResponseDto> updateOrder(@PathVariable Long orderId, @Valid @RequestBody OrderRequestDto orderRequestDto) {
        OrderResponseDto updatedOrder = orderService.updateOrder(orderId, orderRequestDto);
        return ResponseEntity.ok(updatedOrder);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORDER_ADMIN')")
    @GetMapping
    @Operation(summary = "Get All Orders",description = "Only SUPER_ADMIN and ORDER_ADMIN can see all orders")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "All Orders Found",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = OrderResponseDto.class))
                    )
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content)
    })
    public ResponseEntity<List<OrderResponseDto>> findAllOrders() {
        List<OrderResponseDto> orders = orderService.findAllOrders();
        return ResponseEntity.ok(orders);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORDER_ADMIN') or @orderSecurityService.isOrderOwner(#orderId)")
    @GetMapping("/{orderId}")
    @Operation(summary = "Get Order by its orderId",description = "SUPER_ADMIN,ORDER_ADMIN and user itself can access this")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Order Found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponseDto.class)
                    )
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content),
            @ApiResponse(responseCode = "404",description = "Not Found",content = @Content)
    })
    public ResponseEntity<OrderResponseDto> findOrderById(@PathVariable Long orderId) {
        OrderResponseDto order = orderService.findOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORDER_ADMIN','DELIVERY_ADMIN')")
    @GetMapping("/{orderId}/delivery")
    @Operation(summary = "Get delivery details of a order",description = "ONLY SUPER_ADMIN,ORDER_ADMIN and DELIVERY_ADMIN can see this")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Delivery Found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DeliveryDto.class)
                    )
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content),
            @ApiResponse(responseCode = "404",description = "Not Found",content = @Content)
    })
    public ResponseEntity<DeliveryDto> findDeliveryByOrderId(@PathVariable Long orderId) {
        DeliveryDto delivery = orderService.getDeliveryByOrderId(orderId);
        return ResponseEntity.ok(delivery);
    }
}
