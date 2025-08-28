/*
 * Copyright 2016 Kevin Herron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ksh.modbus;

import java.util.concurrent.ExecutionException;

import com.digitalpetri.modbus.requests.*;
import com.digitalpetri.modbus.responses.*;
import com.digitalpetri.modbus.slave.ModbusTcpSlave;
import com.digitalpetri.modbus.slave.ModbusTcpSlaveConfig;
import com.digitalpetri.modbus.slave.ServiceRequestHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlaveExample {

    // Define data - Modbus data model with configurable sizes
    private static final int COIL_COUNT = 65536;
    private static final int DISCRETE_INPUT_COUNT = 65536;
    private static final int HOLDING_REGISTER_COUNT = 65536;
    private static final int INPUT_REGISTER_COUNT = 65536;
    
    // Data storage arrays
    private final boolean[] coils;
    private final boolean[] discreteInputs;
    private final short[] holdingRegisters;
    private final short[] inputRegisters;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final SlaveExample slaveExample = new SlaveExample();

        slaveExample.start();

        Runtime.getRuntime().addShutdownHook(new Thread("modbus-slave-shutdown-hook") {
            @Override
            public void run() {
                slaveExample.stop();
            }
        });

        Thread.sleep(Integer.MAX_VALUE);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ModbusTcpSlaveConfig config = new ModbusTcpSlaveConfig.Builder().build();
    private final ModbusTcpSlave slave = new ModbusTcpSlave(config);

    public SlaveExample() {
        // Initialize data arrays
        coils = new boolean[COIL_COUNT];
        discreteInputs = new boolean[DISCRETE_INPUT_COUNT];
        holdingRegisters = new short[HOLDING_REGISTER_COUNT];
        inputRegisters = new short[INPUT_REGISTER_COUNT];
        
        // Initialize with some test data
        initializeTestData();
    }

    private void initializeTestData() {
        // Initialize some test data for demonstration
        for (int i = 0; i < 100; i++) {
            coils[i] = (i % 2 == 0);
            discreteInputs[i] = (i % 3 == 0);
            holdingRegisters[i] = (short) (i * 10);
            inputRegisters[i] = (short) (i * 5 + 1000);
        }
        logger.info("Test data initialized");
    }

    public void start() throws ExecutionException, InterruptedException {
        slave.setRequestHandler(new ServiceRequestHandler() {
            
            // 0x01 - Read Coils
            @Override
            public void onReadCoils(ServiceRequest<ReadCoilsRequest, ReadCoilsResponse> service) {
                ReadCoilsRequest request = service.getRequest();
                int address = request.getAddress();
                int quantity = request.getQuantity();
                
                logger.info("Read Coils - Address: {}, Quantity: {}", address, quantity);
                
                try {
                    if (address < 0 || address >= COIL_COUNT || quantity <= 0 || address + quantity > COIL_COUNT) {
                        // Create minimal error response
                        ByteBuf errorResponse = PooledByteBufAllocator.DEFAULT.buffer(1);
                        errorResponse.writeByte(0);
                        service.sendResponse(new ReadCoilsResponse(errorResponse));
                        return;
                    }
                    
                    int byteCount = (quantity + 7) / 8;
                    ByteBuf coilStatus = PooledByteBufAllocator.DEFAULT.buffer(byteCount);
                    
                    for (int byteIndex = 0; byteIndex < byteCount; byteIndex++) {
                        byte coilByte = 0;
                        for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                            int coilIndex = address + (byteIndex * 8) + bitIndex;
                            if (coilIndex < address + quantity && coils[coilIndex]) {
                                coilByte |= (1 << bitIndex);
                            }
                        }
                        coilStatus.writeByte(coilByte);
                    }
                    
                    service.sendResponse(new ReadCoilsResponse(coilStatus));
                } finally {
                    ReferenceCountUtil.release(request);
                }
            }
            
            // 0x02 - Read Discrete Inputs
            @Override
            public void onReadDiscreteInputs(ServiceRequest<ReadDiscreteInputsRequest, ReadDiscreteInputsResponse> service) {
                ReadDiscreteInputsRequest request = service.getRequest();
                int address = request.getAddress();
                int quantity = request.getQuantity();
                
                logger.info("Read Discrete Inputs - Address: {}, Quantity: {}", address, quantity);
                
                try {
                    if (address < 0 || address >= DISCRETE_INPUT_COUNT || quantity <= 0 || address + quantity > DISCRETE_INPUT_COUNT) {
                        ByteBuf errorResponse = PooledByteBufAllocator.DEFAULT.buffer(1);
                        errorResponse.writeByte(0);
                        service.sendResponse(new ReadDiscreteInputsResponse(errorResponse));
                        return;
                    }
                    
                    int byteCount = (quantity + 7) / 8;
                    ByteBuf inputStatus = PooledByteBufAllocator.DEFAULT.buffer(byteCount);
                    
                    for (int byteIndex = 0; byteIndex < byteCount; byteIndex++) {
                        byte inputByte = 0;
                        for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                            int inputIndex = address + (byteIndex * 8) + bitIndex;
                            if (inputIndex < address + quantity && discreteInputs[inputIndex]) {
                                inputByte |= (1 << bitIndex);
                            }
                        }
                        inputStatus.writeByte(inputByte);
                    }
                    
                    service.sendResponse(new ReadDiscreteInputsResponse(inputStatus));
                } finally {
                    ReferenceCountUtil.release(request);
                }
            }

            // 0x03 - Read Holding Registers
            @Override
            public void onReadHoldingRegisters(ServiceRequest<ReadHoldingRegistersRequest, ReadHoldingRegistersResponse> service) {
                String clientRemoteAddress = service.getChannel().remoteAddress().toString();
                String clientIp = clientRemoteAddress.replaceAll(".*/(.*):.*", "$1");
                String clientPort = clientRemoteAddress.replaceAll(".*:(.*)", "$1");

                ReadHoldingRegistersRequest request = service.getRequest();
                int address = request.getAddress();
                int quantity = request.getQuantity();
                
                logger.info("Read Holding Registers from {}:{} - Address: {}, Quantity: {}", 
                           clientIp, clientPort, address, quantity);

                try {
                    if (address < 0 || address >= HOLDING_REGISTER_COUNT || quantity <= 0 || address + quantity > HOLDING_REGISTER_COUNT) {
                        ByteBuf errorResponse = PooledByteBufAllocator.DEFAULT.buffer(2);
                        errorResponse.writeShort(0);
                        service.sendResponse(new ReadHoldingRegistersResponse(errorResponse));
                        return;
                    }
                    
                    ByteBuf registers = PooledByteBufAllocator.DEFAULT.buffer(quantity * 2);

                    for (int i = 0; i < quantity; i++) {
                        registers.writeShort(holdingRegisters[address + i]);
                    }

                    service.sendResponse(new ReadHoldingRegistersResponse(registers));
                } finally {
                    ReferenceCountUtil.release(request);
                }
            }
            
            // 0x04 - Read Input Registers
            @Override
            public void onReadInputRegisters(ServiceRequest<ReadInputRegistersRequest, ReadInputRegistersResponse> service) {
                ReadInputRegistersRequest request = service.getRequest();
                int address = request.getAddress();
                int quantity = request.getQuantity();
                
                logger.info("Read Input Registers - Address: {}, Quantity: {}", address, quantity);
                
                try {
                    if (address < 0 || address >= INPUT_REGISTER_COUNT || quantity <= 0 || address + quantity > INPUT_REGISTER_COUNT) {
                        ByteBuf errorResponse = PooledByteBufAllocator.DEFAULT.buffer(2);
                        errorResponse.writeShort(0);
                        service.sendResponse(new ReadInputRegistersResponse(errorResponse));
                        return;
                    }
                    
                    ByteBuf registers = PooledByteBufAllocator.DEFAULT.buffer(quantity * 2);

                    for (int i = 0; i < quantity; i++) {
                        registers.writeShort(inputRegisters[address + i]);
                    }

                    service.sendResponse(new ReadInputRegistersResponse(registers));
                } finally {
                    ReferenceCountUtil.release(request);
                }
            }
            
            // 0x05 - Write Single Coil
            @Override
            public void onWriteSingleCoil(ServiceRequest<WriteSingleCoilRequest, WriteSingleCoilResponse> service) {
                WriteSingleCoilRequest request = service.getRequest();
                int address = request.getAddress();
                int value = request.getValue();
                boolean coilValue = (value == 0xFF00);
                
                logger.info("Write Single Coil - Address: {}, Value: {} ({})", address, value, coilValue);
                
                try {
                    if (address < 0 || address >= COIL_COUNT) {
                        // Send error response - create a response with the original values
                        service.sendResponse(new WriteSingleCoilResponse(address, value));
                        return;
                    }
                    
                    coils[address] = coilValue;
                    service.sendResponse(new WriteSingleCoilResponse(address, value));
                } finally {
                    ReferenceCountUtil.release(request);
                }
            }
            
            // 0x06 - Write Single Register
            @Override
            public void onWriteSingleRegister(ServiceRequest<WriteSingleRegisterRequest, WriteSingleRegisterResponse> service) {
                WriteSingleRegisterRequest request = service.getRequest();
                int address = request.getAddress();
                int value = request.getValue();
                
                logger.info("Write Single Register - Address: {}, Value: {}", address, value);
                
                try {
                    if (address < 0 || address >= HOLDING_REGISTER_COUNT) {
                        service.sendResponse(new WriteSingleRegisterResponse(address, value));
                        return;
                    }
                    
                    holdingRegisters[address] = (short) value;
                    service.sendResponse(new WriteSingleRegisterResponse(address, value));
                } finally {
                    ReferenceCountUtil.release(request);
                }
            }
            
            // 0x0F - Write Multiple Coils
            @Override
            public void onWriteMultipleCoils(ServiceRequest<WriteMultipleCoilsRequest, WriteMultipleCoilsResponse> service) {
                WriteMultipleCoilsRequest request = service.getRequest();
                int address = request.getAddress();
                int quantity = request.getQuantity();
                ByteBuf values = request.getValues();
                
                logger.info("Write Multiple Coils - Address: {}, Quantity: {}", address, quantity);
                
                try {
                    if (address < 0 || address >= COIL_COUNT || quantity <= 0 || address + quantity > COIL_COUNT) {
                        service.sendResponse(new WriteMultipleCoilsResponse(address, quantity));
                        return;
                    }
                    
                    for (int i = 0; i < quantity; i++) {
                        int byteIndex = i / 8;
                        int bitIndex = i % 8;
                        byte coilByte = values.getByte(byteIndex);
                        boolean coilValue = ((coilByte >> bitIndex) & 0x01) == 1;
                        coils[address + i] = coilValue;
                    }
                    
                    service.sendResponse(new WriteMultipleCoilsResponse(address, quantity));
                } finally {
                    ReferenceCountUtil.release(request);
                }
            }
            
            // 0x10 - Write Multiple Registers
            @Override
            public void onWriteMultipleRegisters(ServiceRequest<WriteMultipleRegistersRequest, WriteMultipleRegistersResponse> service) {
                WriteMultipleRegistersRequest request = service.getRequest();
                int address = request.getAddress();
                int quantity = request.getQuantity();
                ByteBuf values = request.getValues();
                
                logger.info("Write Multiple Registers - Address: {}, Quantity: {}", address, quantity);
                
                try {
                    if (address < 0 || address >= HOLDING_REGISTER_COUNT || quantity <= 0 || address + quantity > HOLDING_REGISTER_COUNT) {
                        service.sendResponse(new WriteMultipleRegistersResponse(address, quantity));
                        return;
                    }
                    
                    for (int i = 0; i < quantity; i++) {
                        holdingRegisters[address + i] = values.readShort();
                    }
                    
                    service.sendResponse(new WriteMultipleRegistersResponse(address, quantity));
                } finally {
                    ReferenceCountUtil.release(request);
                }
            }
            
            // 0x16 - Mask Write Register
            @Override
            public void onMaskWriteRegister(ServiceRequest<MaskWriteRegisterRequest, MaskWriteRegisterResponse> service) {
                MaskWriteRegisterRequest request = service.getRequest();
                int address = request.getAddress();
                int andMask = request.getAndMask();
                int orMask = request.getOrMask();
                
                logger.info("Mask Write Register - Address: {}, AND Mask: 0x{}, OR Mask: 0x{}", 
                           address, Integer.toHexString(andMask), Integer.toHexString(orMask));
                
                try {
                    if (address < 0 || address >= HOLDING_REGISTER_COUNT) {
                        service.sendResponse(new MaskWriteRegisterResponse(address, andMask, orMask));
                        return;
                    }
                    
                    int currentValue = holdingRegisters[address] & 0xFFFF;
                    int newValue = (currentValue & andMask) | (orMask & (~andMask));
                    holdingRegisters[address] = (short) newValue;
                    
                    logger.info("Mask Write Register - Address: {}, Old Value: 0x{}, New Value: 0x{}", 
                               address, Integer.toHexString(currentValue), Integer.toHexString(newValue));
                    
                    service.sendResponse(new MaskWriteRegisterResponse(address, andMask, orMask));
                } finally {
                    ReferenceCountUtil.release(request);
                }
            }
            
            // 0x17 - Read/Write Multiple Registers
            @Override
            public void onReadWriteMultipleRegisters(ServiceRequest<ReadWriteMultipleRegistersRequest, ReadWriteMultipleRegistersResponse> service) {
                ReadWriteMultipleRegistersRequest request = service.getRequest();
                int readAddress = request.getReadAddress();
                int readQuantity = request.getReadQuantity();
                int writeAddress = request.getWriteAddress();
                int writeQuantity = request.getWriteQuantity();
                ByteBuf writeValues = request.getValues();
                
                logger.info("Read/Write Multiple Registers - Read Addr: {}, Read Qty: {}, Write Addr: {}, Write Qty: {}", 
                           readAddress, readQuantity, writeAddress, writeQuantity);
                
                try {
                    if (readAddress < 0 || readAddress >= HOLDING_REGISTER_COUNT || readQuantity <= 0 ||
                        readAddress + readQuantity > HOLDING_REGISTER_COUNT ||
                        writeAddress < 0 || writeAddress >= HOLDING_REGISTER_COUNT || writeQuantity <= 0 ||
                        writeAddress + writeQuantity > HOLDING_REGISTER_COUNT) {
                        
                        // Create minimal read response
                        ByteBuf errorReadValues = PooledByteBufAllocator.DEFAULT.buffer(2);
                        errorReadValues.writeShort(0);
                        service.sendResponse(new ReadWriteMultipleRegistersResponse(errorReadValues));
                        return;
                    }
                    
                    // First perform write operation
                    for (int i = 0; i < writeQuantity; i++) {
                        holdingRegisters[writeAddress + i] = writeValues.readShort();
                    }
                    
                    // Then perform read operation
                    ByteBuf readValues = PooledByteBufAllocator.DEFAULT.buffer(readQuantity * 2);
                    for (int i = 0; i < readQuantity; i++) {
                        readValues.writeShort(holdingRegisters[readAddress + i]);
                    }
                    
                    service.sendResponse(new ReadWriteMultipleRegistersResponse(readValues));
                } finally {
                    ReferenceCountUtil.release(request);
                }
            }
        });

        slave.bind("localhost", 50200).get();
        logger.info("Modbus TCP Slave started on localhost:50200 with full function support");
        logger.info("Supported functions: 01, 02, 03, 04, 05, 06, 0F, 10, 16, 17");
    }

    public void stop() {
        slave.shutdown();
        logger.info("Modbus TCP Slave stopped");
    }
    
    // Helper methods for accessing data (useful for testing or external access)
    public boolean getCoil(int address) {
        return address >= 0 && address < COIL_COUNT ? coils[address] : false;
    }
    
    public void setCoil(int address, boolean value) {
        if (address >= 0 && address < COIL_COUNT) {
            coils[address] = value;
            logger.debug("Set coil[{}] = {}", address, value);
        }
    }
    
    public boolean getDiscreteInput(int address) {
        return address >= 0 && address < DISCRETE_INPUT_COUNT ? discreteInputs[address] : false;
    }
    
    public void setDiscreteInput(int address, boolean value) {
        if (address >= 0 && address < DISCRETE_INPUT_COUNT) {
            discreteInputs[address] = value;
            logger.debug("Set discreteInput[{}] = {}", address, value);
        }
    }
    
    public short getHoldingRegister(int address) {
        return address >= 0 && address < HOLDING_REGISTER_COUNT ? holdingRegisters[address] : 0;
    }
    
    public void setHoldingRegister(int address, short value) {
        if (address >= 0 && address < HOLDING_REGISTER_COUNT) {
            holdingRegisters[address] = value;
            logger.debug("Set holdingRegister[{}] = {}", address, value);
        }
    }
    
    public short getInputRegister(int address) {
        return address >= 0 && address < INPUT_REGISTER_COUNT ? inputRegisters[address] : 0;
    }
    
    public void setInputRegister(int address, short value) {
        if (address >= 0 && address < INPUT_REGISTER_COUNT) {
            inputRegisters[address] = value;
            logger.debug("Set inputRegister[{}] = {}", address, value);
        }
    }
}