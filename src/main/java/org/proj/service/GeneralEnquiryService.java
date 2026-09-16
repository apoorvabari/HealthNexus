package org.proj.service;

import org.proj.dto.GeneralEnquiryRequest;
import org.proj.dto.GeneralEnquiryResponse;

import java.util.List;
import java.util.UUID;

public interface GeneralEnquiryService {

    GeneralEnquiryResponse createEnquiry(GeneralEnquiryRequest request);

    List<GeneralEnquiryResponse> getHospitalEnquiries();

    GeneralEnquiryResponse updateEnquiryStatus(UUID enquiryId, GeneralEnquiryRequest request);
}
