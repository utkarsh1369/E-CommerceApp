package com.microservices.delivery_service.controller;

import com.microservices.delivery_service.model.dto.DeliveryDto;
import com.microservices.delivery_service.model.dto.DeliveryRequestDto;
import com.microservices.delivery_service.model.dto.OrderDto;
import com.microservices.delivery_service.model.dto.UpdateDeliveryStatusDto;
import com.microservices.delivery_service.service.DeliveryService;
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
@RequestMapping("/api/v1/delivery")
@Tag(name = "Delivery APIs",description = "CRUD Operations on Deliveries")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PreAuthorize("hasRole('DELIVERY_ADMIN')")
    @PostMapping("/create")
    @Operation(summary = "Create a Delivery",description = "Only DELIVERY_ADMIN can create a delivery")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Delivery Created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DeliveryDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400",description = "Bad Request",content = @Content),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content)
    })
    public ResponseEntity<DeliveryDto> createDelivery(@Valid @RequestBody DeliveryRequestDto deliveryRequestDto) {
        DeliveryDto createdDelivery = deliveryService.createDelivery(deliveryRequestDto);
        return new ResponseEntity<>(createdDelivery, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('DELIVERY_ADMIN')")
    @PatchMapping("/{deliveryId}/update-status")
    @Operation(summary = "Update Status of Delivery",description = "Only DELIVERY_ADMIN can update the status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Status Updated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DeliveryDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400",description = "Bad Request",content = @Content),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content)
    })
    public ResponseEntity<DeliveryDto> updateDeliveryStatus(@PathVariable Long deliveryId, @Valid @RequestBody UpdateDeliveryStatusDto statusDto) {
        DeliveryDto updatedDelivery = deliveryService.updateDeliveryStatus(deliveryId, statusDto);
        return ResponseEntity.ok(updatedDelivery);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DELIVERY_ADMIN')")
    @GetMapping
    @Operation(summary = "Get All deliveries",description = "Only SUPER_ADMIN and DELIVERY_ADMIN can see all orders")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found All Deliveries",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DeliveryDto.class))
                    )
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content)
    })
    public ResponseEntity<List<DeliveryDto>> getAllDeliveries() {
        List<DeliveryDto> deliveries = deliveryService.findAllDeliveries();
        return ResponseEntity.ok(deliveries);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DELIVERY_ADMIN') or @deliverySecurityService.isDeliveryOwner(#deliveryId)")
    @GetMapping("/{deliveryId}")
    @Operation(summary = "Get Delivery by its deliveryId",description = "Only SUPER_ADMIN,DELIVERY_ADMIN and USER itself can see it")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Order Found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DeliveryDto.class)
                    )
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content),
            @ApiResponse(responseCode = "404",description = "Not Found",content = @Content)
    })
    public ResponseEntity<DeliveryDto> getDeliveryById(@PathVariable Long deliveryId) {
        DeliveryDto delivery = deliveryService.findDeliveryById(deliveryId);
        return ResponseEntity.ok(delivery);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DELIVERY_ADMIN','ORDER_ADMIN')")
    @GetMapping("/{deliveryId}/order")
    @Operation(summary = "Get order of Delivery",description = "Only SUPER_ADMIN,DELIVERY_ADMIN or ORDER_ADMIN can see this")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Order Found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrderDto.class)
                    )
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content),
            @ApiResponse(responseCode = "404",description = "Not Found",content = @Content)
    })
    public ResponseEntity<OrderDto> getOrderByDeliveryId(@PathVariable Long deliveryId) {
        OrderDto order = deliveryService.findOrderByDeliveryId(deliveryId);
        return ResponseEntity.ok(order);
    }
}