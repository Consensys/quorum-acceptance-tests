package com.quorum.gauge.services;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PrecompiledContractServiceTest {
    @Test
    public void isPrecompiledContract() {
        PrecompiledContractService precompiledContractService = new PrecompiledContractService();
        
        Boolean got = precompiledContractService.isPrecompiledContract("0xed9d02e382b34818e88b88a309c7fe71e65f419d");
        assertThat(got).isFalse();

        got = precompiledContractService.isPrecompiledContract("0x000000000000000000000000000000000000007a");
        assertThat(got).isTrue();

        got = precompiledContractService.isPrecompiledContract("0x000000000000000000000000000000000000007f");
        assertThat(got).isTrue();

        got = precompiledContractService.isPrecompiledContract("0x0000000000000000000000000000000000000080");
        assertThat(got).isFalse();

        got = precompiledContractService.isPrecompiledContract("ed9d02e382b34818e88b88a309c7fe71e65f419d");
        assertThat(got).isFalse();

        got = precompiledContractService.isPrecompiledContract("000000000000000000000000000000000000007a");
        assertThat(got).isTrue();

        got = precompiledContractService.isPrecompiledContract("000000000000000000000000000000000000007f");
        assertThat(got).isTrue();

        got = precompiledContractService.isPrecompiledContract("0000000000000000000000000000000000000080");
        assertThat(got).isFalse();
    }
}
