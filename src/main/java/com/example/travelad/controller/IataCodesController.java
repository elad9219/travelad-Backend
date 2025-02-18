package com.example.travelad.controller;

import com.example.travelad.beans.IataCodeEntry;
import com.example.travelad.utils.IataCodeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IataCodesController {

    @GetMapping("/iata-codes")
    public List<IataCodeEntry> getIataCodes() {
        return IataCodeUtils.getIataCodeEntries();
    }
}
