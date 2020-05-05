package com.quorum.gauge.services;

public class HashicorpVaultSigningServiceTest {

//    @Test
//    public void test() throws IOException {
//        HashicorpVaultSigningService hashicorpVaultSigningService = new HashicorpVaultSigningService();
//        QuorumNetworkProperty.Node node = new QuorumNetworkProperty.Node();
//        node.setUrl("http://localhost:22000");
//        node.setName("Node1");
//
//        final BigInteger nonce = BigInteger.ZERO;
//        final BigInteger DEFAULT_GAS_LIMIT = new BigInteger("47b760", 16);
//
//        final Transaction toSign = new Transaction("0x6038dc01869425004ca0b8370f6c81cf464213b3", nonce, BigInteger.ZERO, DEFAULT_GAS_LIMIT, null, null, "0x000000");
//
//        assertThat(toSign).isNotNull();
//
//        ObjectMapper mapper = new ObjectMapper();
//        System.out.println(mapper.writeValueAsString(toSign));
//
//        List<Object> params = new ArrayList<>();
//        params.add(toSign);
//        params.add("");
//
//        Request<?, EthSignTransaction> request = new Request<>(
//            "personal_signTransaction",
//            params,
//            new HttpService("http://localhost:22000"),
//            EthSignTransaction.class);
//
//        EthSignTransaction resp = request.send();
//        if (resp.hasError()) {
//            throw new RuntimeException(resp.getError().getMessage());
//        }
//        Map<String, Object> result = resp.getResult();
//        assertThat(result).isNotNull();
//    }
}
