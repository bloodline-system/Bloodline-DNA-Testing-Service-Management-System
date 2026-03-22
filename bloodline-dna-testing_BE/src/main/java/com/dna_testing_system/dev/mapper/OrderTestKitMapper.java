package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.OrderTestKitRequest;
import com.dna_testing_system.dev.dto.response.OrderTestKitResponse;
import com.dna_testing_system.dev.entity.OrderKit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OrderTestKitMapper {
    OrderTestKitResponse toOrderTestKitResponse(OrderKit orderKit);
    OrderKit toOrderKit(OrderTestKitRequest orderTestKitRequest);
    void updateOrderKitFromRequest(OrderTestKitRequest orderTestKitRequest,@MappingTarget OrderKit orderKit);
}
