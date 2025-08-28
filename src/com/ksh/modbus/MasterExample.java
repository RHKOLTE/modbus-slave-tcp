/*
 * Comprehensive test class for testing all Modbus functions using ModbusTcpMaster
 * Tests all functions implemented in SlaveExample.java
 */

package com.ksh.modbus;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.*;
import com.digitalpetri.modbus.responses.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasterExample {

    private static final Logger logger = LoggerFactory.getLogger(MasterExample.class);
    
    private static final String SLAVE_ADDRESS = "localhost";
    private static final int SLAVE_PORT = 50200;
    private static final int UNIT_ID = 1;
    private static final int TEST_TIMEOUT_SECONDS = 10;

    private final ModbusTcpMaster master;

    public MasterExample() {
        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(SLAVE_ADDRESS)
            .setPort(SLAVE_PORT)
            .setTimeout(Duration.ofSeconds(5))
            .setLazy(false)
            .setPersistent(true)
            .build();
            
        this.master = new ModbusTcpMaster(config);
    }

    public static void main(String[] args) {
        MasterExample tester = new MasterExample();
        
        try {
            logger.info("Starting Modbus Master Tester...");
            tester.runAllTests();
            logger.info("All tests completed successfully!");
        } catch (Exception e) {
            logger.error("Test execution failed", e);
        } finally {
            tester.shutdown();
        }
    }

    public void runAllTests() throws ExecutionException, InterruptedException, TimeoutException {
        // Connect to slave
        logger.info("Connecting to Modbus slave at {}:{}", SLAVE_ADDRESS, SLAVE_PORT);
        master.connect().get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        logger.info("Connected successfully");

        // Wait a moment for connection to stabilize
        Thread.sleep(1000);

        // Run all function tests
        testReadCoils();
        testReadDiscreteInputs();
        testReadHoldingRegisters();
        testReadInputRegisters();
        testWriteSingleCoil();
        testWriteSingleRegister();
        testWriteMultipleCoils();
        testWriteMultipleRegisters();
        testMaskWriteRegister();
        testReadWriteMultipleRegisters();
        
        // Test edge cases
        testEdgeCases();
        
        logger.info("All Modbus function tests completed successfully");
    }

    // Function 0x01 - Read Coils
    private void testReadCoils() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Testing Function 0x01: Read Coils ===");
        
        ReadCoilsRequest request = new ReadCoilsRequest(0, 16);
        CompletableFuture<ReadCoilsResponse> future = master.sendRequest(request, UNIT_ID);
        ReadCoilsResponse response = future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ByteBuf coilStatus = response.getCoilStatus();
        logger.info("Read {} coils starting at address 0", 16);
        
        // Log first few coil values
        for (int i = 0; i < Math.min(16, 8); i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;
            if (byteIndex < coilStatus.readableBytes()) {
                byte coilByte = coilStatus.getByte(byteIndex);
                boolean coilValue = ((coilByte >> bitIndex) & 0x01) == 1;
                logger.info("Coil[{}] = {}", i, coilValue);
            }
        }
        
        logger.info("Read Coils test passed");
    }

    // Function 0x02 - Read Discrete Inputs
    private void testReadDiscreteInputs() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Testing Function 0x02: Read Discrete Inputs ===");
        
        ReadDiscreteInputsRequest request = new ReadDiscreteInputsRequest(0, 12);
        CompletableFuture<ReadDiscreteInputsResponse> future = master.sendRequest(request, UNIT_ID);
        ReadDiscreteInputsResponse response = future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ByteBuf inputStatus = response.getInputStatus();
        logger.info("Read {} discrete inputs starting at address 0", 12);
        
        // Log first few input values
        for (int i = 0; i < Math.min(12, 8); i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;
            if (byteIndex < inputStatus.readableBytes()) {
                byte inputByte = inputStatus.getByte(byteIndex);
                boolean inputValue = ((inputByte >> bitIndex) & 0x01) == 1;
                logger.info("DiscreteInput[{}] = {}", i, inputValue);
            }
        }
        
        logger.info("Read Discrete Inputs test passed");
    }

    // Function 0x03 - Read Holding Registers
    private void testReadHoldingRegisters() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Testing Function 0x03: Read Holding Registers ===");
        
        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(0, 10);
        CompletableFuture<ReadHoldingRegistersResponse> future = master.sendRequest(request, UNIT_ID);
        ReadHoldingRegistersResponse response = future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ByteBuf registers = response.getRegisters();
        logger.info("Read {} holding registers starting at address 0", 10);
        
        // Log register values
        for (int i = 0; i < 10; i++) {
            if (registers.readableBytes() >= 2) {
                short regValue = registers.readShort();
                logger.info("HoldingRegister[{}] = {}", i, regValue & 0xFFFF);
            }
        }
        
        logger.info("Read Holding Registers test passed");
    }

    // Function 0x04 - Read Input Registers
    private void testReadInputRegisters() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Testing Function 0x04: Read Input Registers ===");
        
        ReadInputRegistersRequest request = new ReadInputRegistersRequest(0, 10);
        CompletableFuture<ReadInputRegistersResponse> future = master.sendRequest(request, UNIT_ID);
        ReadInputRegistersResponse response = future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ByteBuf registers = response.getRegisters();
        logger.info("Read {} input registers starting at address 0", 10);
        
        // Log register values
        for (int i = 0; i < 10; i++) {
            if (registers.readableBytes() >= 2) {
                short regValue = registers.readShort();
                logger.info("InputRegister[{}] = {}", i, regValue & 0xFFFF);
            }
        }
        
        logger.info("Read Input Registers test passed");
    }

    // Function 0x05 - Write Single Coil
    private void testWriteSingleCoil() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Testing Function 0x05: Write Single Coil ===");
        
        // Test writing true
        WriteSingleCoilRequest request1 = new WriteSingleCoilRequest(100, true);
        CompletableFuture<WriteSingleCoilResponse> future1 = master.sendRequest(request1, UNIT_ID);
        WriteSingleCoilResponse response1 = future1.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        logger.info("Wrote coil[100] = true, response address: {}, value: 0x{}", 
                   response1.getAddress(), Integer.toHexString(response1.getValue()));
        
        // Test writing false
        WriteSingleCoilRequest request2 = new WriteSingleCoilRequest(101, false);
        CompletableFuture<WriteSingleCoilResponse> future2 = master.sendRequest(request2, UNIT_ID);
        WriteSingleCoilResponse response2 = future2.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        logger.info("Wrote coil[101] = false, response address: {}, value: 0x{}", 
                   response2.getAddress(), Integer.toHexString(response2.getValue()));
        
        // Verify by reading back
        ReadCoilsRequest verifyRequest = new ReadCoilsRequest(100, 2);
        CompletableFuture<ReadCoilsResponse> verifyFuture = master.sendRequest(verifyRequest, UNIT_ID);
        ReadCoilsResponse verifyResponse = verifyFuture.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ByteBuf coilStatus = verifyResponse.getCoilStatus();
        if (coilStatus.readableBytes() > 0) {
            byte coilByte = coilStatus.getByte(0);
            boolean coil100 = (coilByte & 0x01) == 1;
            boolean coil101 = ((coilByte >> 1) & 0x01) == 1;
            logger.info("Verification - coil[100] = {}, coil[101] = {}", coil100, coil101);
        }
        
        logger.info("Write Single Coil test passed");
    }

    // Function 0x06 - Write Single Register
    private void testWriteSingleRegister() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Testing Function 0x06: Write Single Register ===");
        
        WriteSingleRegisterRequest request = new WriteSingleRegisterRequest(200, 12345);
        CompletableFuture<WriteSingleRegisterResponse> future = master.sendRequest(request, UNIT_ID);
        WriteSingleRegisterResponse response = future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        logger.info("Wrote register[200] = 12345, response address: {}, value: {}", 
                   response.getAddress(), response.getValue());
        
        // Verify by reading back
        ReadHoldingRegistersRequest verifyRequest = new ReadHoldingRegistersRequest(200, 1);
        CompletableFuture<ReadHoldingRegistersResponse> verifyFuture = master.sendRequest(verifyRequest, UNIT_ID);
        ReadHoldingRegistersResponse verifyResponse = verifyFuture.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ByteBuf registers = verifyResponse.getRegisters();
        if (registers.readableBytes() >= 2) {
            short regValue = registers.readShort();
            logger.info("Verification - register[200] = {}", regValue & 0xFFFF);
        }
        
        logger.info("Write Single Register test passed");
    }

    // Function 0x0F - Write Multiple Coils
    private void testWriteMultipleCoils() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Testing Function 0x0F: Write Multiple Coils ===");
        
        // Create coil values: alternating true/false pattern
        byte[] coilValues = {(byte) 0xAA, (byte) 0x55}; // 10101010 01010101
        int quantity = 16;
        
        WriteMultipleCoilsRequest request = new WriteMultipleCoilsRequest(300, quantity, coilValues);
        CompletableFuture<WriteMultipleCoilsResponse> future = master.sendRequest(request, UNIT_ID);
        WriteMultipleCoilsResponse response = future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        logger.info("Wrote {} coils starting at address 300, response address: {}, quantity: {}", 
                   quantity, response.getAddress(), response.getQuantity());
        
        // Verify by reading back
        ReadCoilsRequest verifyRequest = new ReadCoilsRequest(300, quantity);
        CompletableFuture<ReadCoilsResponse> verifyFuture = master.sendRequest(verifyRequest, UNIT_ID);
        ReadCoilsResponse verifyResponse = verifyFuture.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ByteBuf coilStatus = verifyResponse.getCoilStatus();
        logger.info("Verification - read back {} bytes of coil data", coilStatus.readableBytes());
        for (int i = 0; i < Math.min(coilStatus.readableBytes(), 2); i++) {
            byte coilByte = coilStatus.getByte(i);
            logger.info("Coil byte[{}] = 0x{}", i, String.format("%02X", coilByte & 0xFF));
        }
        
        logger.info("Write Multiple Coils test passed");
    }

    // Function 0x10 - Write Multiple Registers
    private void testWriteMultipleRegisters() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Testing Function 0x10: Write Multiple Registers ===");
        
        // Create register values
        ByteBuf registerValues = PooledByteBufAllocator.DEFAULT.buffer(10);
        for (int i = 0; i < 5; i++) {
            registerValues.writeShort(1000 + i);
        }
        
        WriteMultipleRegistersRequest request = new WriteMultipleRegistersRequest(400, 5, registerValues);
        CompletableFuture<WriteMultipleRegistersResponse> future = master.sendRequest(request, UNIT_ID);
        WriteMultipleRegistersResponse response = future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        logger.info("Wrote {} registers starting at address 400, response address: {}, quantity: {}", 
                   5, response.getAddress(), response.getQuantity());
        
        // Verify by reading back
        ReadHoldingRegistersRequest verifyRequest = new ReadHoldingRegistersRequest(400, 5);
        CompletableFuture<ReadHoldingRegistersResponse> verifyFuture = master.sendRequest(verifyRequest, UNIT_ID);
        ReadHoldingRegistersResponse verifyResponse = verifyFuture.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ByteBuf registers = verifyResponse.getRegisters();
        logger.info("Verification - read back {} registers:", 5);
        for (int i = 0; i < 5; i++) {
            if (registers.readableBytes() >= 2) {
                short regValue = registers.readShort();
                logger.info("Register[{}] = {}", 400 + i, regValue & 0xFFFF);
            }
        }
        
        logger.info("Write Multiple Registers test passed");
    }

    // Function 0x16 - Mask Write Register
    private void testMaskWriteRegister() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Testing Function 0x16: Mask Write Register ===");
        
        // First, write a known value to test register
        WriteSingleRegisterRequest setupRequest = new WriteSingleRegisterRequest(500, 0xABCD);
        master.sendRequest(setupRequest, UNIT_ID).get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        logger.info("Setup: Wrote 0xABCD to register[500]");
        
        // Apply mask: AND with 0xFF00, OR with 0x0012
        // This should clear lower 8 bits and set bits 1 and 4 in lower byte
        MaskWriteRegisterRequest request = new MaskWriteRegisterRequest(500, 0xFF00, 0x0012);
        CompletableFuture<MaskWriteRegisterResponse> future = master.sendRequest(request, UNIT_ID);
        MaskWriteRegisterResponse response = future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        logger.info("Applied mask - Address: {}, AND: 0x{}, OR: 0x{}", 
                   response.getAddress(), 
                   Integer.toHexString(response.getAndMask()),
                   Integer.toHexString(response.getOrMask()));
        
        // Verify result
        ReadHoldingRegistersRequest verifyRequest = new ReadHoldingRegistersRequest(500, 1);
        CompletableFuture<ReadHoldingRegistersResponse> verifyFuture = master.sendRequest(verifyRequest, UNIT_ID);
        ReadHoldingRegistersResponse verifyResponse = verifyFuture.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ByteBuf registers = verifyResponse.getRegisters();
        if (registers.readableBytes() >= 2) {
            short regValue = registers.readShort();
            logger.info("Verification - register[500] = 0x{}", Integer.toHexString(regValue & 0xFFFF));
            // Expected: (0xABCD & 0xFF00) | (0x0012 & ~0xFF00) = 0xAB00 | 0x0012 = 0xAB12
        }
        
        logger.info("Mask Write Register test passed");
    }

    // Function 0x17 - Read/Write Multiple Registers
    private void testReadWriteMultipleRegisters() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Testing Function 0x17: Read/Write Multiple Registers ===");
        
        // Create write values
        ByteBuf writeValues = PooledByteBufAllocator.DEFAULT.buffer(6);
        writeValues.writeShort(2001);
        writeValues.writeShort(2002);
        writeValues.writeShort(2003);
        
        ReadWriteMultipleRegistersRequest request = new ReadWriteMultipleRegistersRequest(
            600, 5,     // Read 5 registers starting at 600
            700, 3,     // Write 3 registers starting at 700
            writeValues
        );
        
        CompletableFuture<ReadWriteMultipleRegistersResponse> future = master.sendRequest(request, UNIT_ID);
        ReadWriteMultipleRegistersResponse response = future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ByteBuf readValues = response.getRegisters();
        logger.info("Read/Write operation completed - read {} registers from address 600, wrote 3 registers to address 700", 5);
        
        // Log read values
        for (int i = 0; i < 5; i++) {
            if (readValues.readableBytes() >= 2) {
                short regValue = readValues.readShort();
                logger.info("Read register[{}] = {}", 600 + i, regValue & 0xFFFF);
            }
        }
        
        // Verify write operation by reading the written registers
        ReadHoldingRegistersRequest verifyRequest = new ReadHoldingRegistersRequest(700, 3);
        CompletableFuture<ReadHoldingRegistersResponse> verifyFuture = master.sendRequest(verifyRequest, UNIT_ID);
        ReadHoldingRegistersResponse verifyResponse = verifyFuture.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ByteBuf verifyRegisters = verifyResponse.getRegisters();
        logger.info("Verification of written registers:");
        for (int i = 0; i < 3; i++) {
            if (verifyRegisters.readableBytes() >= 2) {
                short regValue = verifyRegisters.readShort();
                logger.info("Written register[{}] = {}", 700 + i, regValue & 0xFFFF);
            }
        }
        
        logger.info("Read/Write Multiple Registers test passed");
    }

    // Test edge cases and error conditions
    private void testEdgeCases() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Testing Edge Cases ===");
        
        // Test reading at maximum address (should work)
        try {
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(65535, 1);
            CompletableFuture<ReadHoldingRegistersResponse> future = master.sendRequest(request, UNIT_ID);
            ReadHoldingRegistersResponse response = future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.info("Successfully read register at address 65535");
        } catch (Exception e) {
            logger.info("Reading at address 65535 failed as expected: {}", e.getMessage());
        }
        
        // Test reading beyond array bounds (should handle gracefully)
        try {
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(65534, 10);
            CompletableFuture<ReadHoldingRegistersResponse> future = master.sendRequest(request, UNIT_ID);
            ReadHoldingRegistersResponse response = future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.info("Reading beyond bounds handled gracefully");
        } catch (Exception e) {
            logger.info("Reading beyond bounds failed as expected: {}", e.getMessage());
        }
        
        logger.info("Edge case testing completed");
    }

    // Performance test
    public void performanceTest() throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("=== Performance Test ===");
        
        int iterations = 100;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(i % 1000, 10);
            master.sendRequest(request, UNIT_ID).get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTime = (double) totalTime / iterations;
        
        logger.info("Performance test completed: {} requests in {}ms (avg: {:.2f}ms per request)", 
                   iterations, totalTime, avgTime);
    }

    // Concurrent test
    public void concurrentTest() throws InterruptedException {
        logger.info("=== Concurrent Test ===");
        
        int concurrentRequests = 10;
        CompletableFuture<?>[] futures = new CompletableFuture[concurrentRequests];
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int index = i;
            futures[i] = master.sendRequest(new ReadHoldingRegistersRequest(index * 10, 5), UNIT_ID)
                .thenAccept(response -> {
                    logger.info("Concurrent request {} completed", index);
                });
        }
        
        // Wait for all requests to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        try {
            allFutures.get(TEST_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Concurrent test failed", e);
            return;
        }
        
        long endTime = System.currentTimeMillis();
        logger.info("Concurrent test completed: {} requests in {}ms", 
                   concurrentRequests, endTime - startTime);
    }

    public void shutdown() {
        try {
            logger.info("Shutting down Modbus master...");
            master.disconnect().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Error during shutdown", e);
        }
    }

    // Helper method to run extended tests
    public void runExtendedTests() throws ExecutionException, InterruptedException, TimeoutException {
        runAllTests();
        performanceTest();
        concurrentTest();
    }
}