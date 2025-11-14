package com.microservice.product_service.controller;

import com.microservice.product_service.model.dto.ProductDTO;
import com.microservice.product_service.service.ProductService;
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
@RequestMapping("/products")
@Tag(name = "Product APIs",description = "CRUD operation on Products")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRODUCT_ADMIN')")
    @PostMapping("/add")
    @Operation(summary = "Add Products",description = "Only SUPER_ADMIN or PRODUCT_ADMIN can add products.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Product Created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "400",description = "Bad Request",content = @Content),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content)
    })
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        ProductDTO createdProduct = productService.createProduct(productDTO);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRODUCT_ADMIN')")
    @PutMapping("/update/{productId}")
    @Operation(summary = "Update Product",description = "Only SUPER_ADMIN or PRODUCT_ADMIN has permission")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Product Updated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "400",description = "Bad Request",content = @Content),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content)
    })
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = productService.updateProduct(productId, productDTO);
        return ResponseEntity.ok(updatedProduct);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRODUCT_ADMIN', 'USER','ORDER_ADMIN')")
    @GetMapping
    @Operation(summary = "Get all Products",description = "Only SUPER_ADMIN,PRODUCT_ADMIN and ORDER_ADMIN has permission")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "All Products Found",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProductDTO.class))
                    )
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content)
    })
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<ProductDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRODUCT_ADMIN', 'USER','ORDER_ADMIN')")
    @GetMapping("/{productId}")
    @Operation(summary = "Get Product by productId",description = "SUPER_ADMIN,PRODUCT_ADMIN,ORDER_ADMIN and USER has permission")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Product Found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content),
            @ApiResponse(responseCode = "404",description = "Not Found",content = @Content)
    })
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long productId) {
        ProductDTO product = productService.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRODUCT_ADMIN')")
    @DeleteMapping("/delete/{productId}")
    @Operation(summary = "Delete a Product",description = "Only SUPER_ADMIN and PRODUCT_ADMIN has permission")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204",
                    description = "No Content"
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content),
            @ApiResponse(responseCode = "404",description = "Not Found",content = @Content)
    })
    public ResponseEntity<Void> deleteProductById(@PathVariable Long productId) {
        productService.deleteProductById(productId);
        return ResponseEntity.noContent().build();
    }
}