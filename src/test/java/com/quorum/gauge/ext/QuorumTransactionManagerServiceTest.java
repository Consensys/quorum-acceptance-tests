package com.quorum.gauge.ext;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuorumTransactionManagerServiceTest {
    // A simple unit test to verify the logic in QuorumTransactionManagerService.isPrecompiledContract
    @Test
    public void isPrecompiledContract() {
        Boolean got = QuorumTransactionManagerService.isPrecompiledContract("0xed9d02e382b34818e88b88a309c7fe71e65f419d");
        assertThat(got).isFalse();

        got = QuorumTransactionManagerService.isPrecompiledContract("0x000000000000000000000000000000000000007a");
        assertThat(got).isTrue();

        got = QuorumTransactionManagerService.isPrecompiledContract("0x000000000000000000000000000000000000007f");
        assertThat(got).isTrue();

        got = QuorumTransactionManagerService.isPrecompiledContract("0x0000000000000000000000000000000000000080");
        assertThat(got).isFalse();

        got = QuorumTransactionManagerService.isPrecompiledContract("ed9d02e382b34818e88b88a309c7fe71e65f419d");
        assertThat(got).isFalse();

        got = QuorumTransactionManagerService.isPrecompiledContract("000000000000000000000000000000000000007a");
        assertThat(got).isTrue();

        got = QuorumTransactionManagerService.isPrecompiledContract("000000000000000000000000000000000000007f");
        assertThat(got).isTrue();

        got = QuorumTransactionManagerService.isPrecompiledContract("0000000000000000000000000000000000000080");
        assertThat(got).isFalse();
    }
}
