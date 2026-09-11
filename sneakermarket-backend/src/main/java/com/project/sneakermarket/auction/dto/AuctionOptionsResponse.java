package com.project.sneakermarket.auction.dto;
import java.util.List;
public record AuctionOptionsResponse(List<AuctionLookupOption> brands, List<AuctionLookupOption> categories) {}
