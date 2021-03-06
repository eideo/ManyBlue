package io.javac.ManyBlue.callback;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.UUID;

import io.javac.ManyBlue.bean.NotifyMessage;
import io.javac.ManyBlue.bean.UUIDMessage;
import io.javac.ManyBlue.code.CodeUtils;
import io.javac.ManyBlue.manager.EventManager;
import io.javac.ManyBlue.utils.HexUtils;

/**
 * Created by Pencilso on 2017/7/22.
 */

public class BlueGattCallBack extends BluetoothGattCallback {
    private Object tag;
    private BluetoothGatt bluetoothGatt;//连接的gatt
    private BluetoothGattService device_service;//服务通道
    private BluetoothGattCharacteristic characteristic_write;//写出的通道
    private BluetoothGattCharacteristic characteristic_read;//读取通道
    private int code;//读取数据方式code


    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    /**
     * 蓝牙设备连接状态被改变
     *
     * @param gatt
     * @param status
     * @param newState
     */
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        NotifyMessage notifyMessage = new NotifyMessage();
        notifyMessage.setTag(tag);
        notifyMessage.setCode(CodeUtils.SERVICE_ONCONNEXT_STATE);
        switch (newState) {//对蓝牙反馈的状态进行判断
            case BluetoothProfile.STATE_CONNECTED://已链接
                gatt.discoverServices();//调用发现设备中的服务
                notifyMessage.setData(true);
                break;
            case BluetoothProfile.STATE_DISCONNECTED://已断开
                notifyMessage.setData(false);
                break;
        }
        EventManager.getLibraryEvent().post(notifyMessage);
    }

    /**
     * 发现服务后回调方法
     *
     * @param gatt
     * @param status
     */
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        /**
         * 遍历所有发现到的服务 把所有服务回调到EventBus当中
         */
        bluetoothGatt = gatt;
        List<BluetoothGattService> services = gatt.getServices();
        NotifyMessage notifyMessage = new NotifyMessage(CodeUtils.SERVICE_ONSERVICESDISCOVERED, services, tag);
        EventManager.getLibraryEvent().post(notifyMessage);

    }

    public void registerDevice(UUIDMessage uuidMessage) {
        NotifyMessage notifyMessage = new NotifyMessage();
        notifyMessage.setCode(CodeUtils.SERVICE_ONREGISTER_DEVICE);
        try {
            if (uuidMessage == null) return;
            if (uuidMessage.getCharac_uuid_service() != null) {
                device_service = bluetoothGatt.getService(UUID.fromString(uuidMessage.getCharac_uuid_service()));
                if (device_service != null) {
                    if (uuidMessage.getCharac_uuid_write() != null)
                        characteristic_write = device_service.getCharacteristic(UUID.fromString(uuidMessage.getCharac_uuid_write()));
                    if (uuidMessage.getCharac_uuid_read() != null) {
                        characteristic_read = device_service.getCharacteristic(UUID.fromString(uuidMessage.getCharac_uuid_read()));
                        if (characteristic_read != null && uuidMessage.getDescriptor_uuid_notify() != null) {//注册Notify通知
                            bluetoothGatt.setCharacteristicNotification(characteristic_read, true);
                            BluetoothGattDescriptor descriptor = characteristic_read.getDescriptor(UUID.fromString(uuidMessage.getDescriptor_uuid_notify()));
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            bluetoothGatt.writeDescriptor(descriptor);
                        }
                    }
                }
            }
            notifyMessage.setData(true);
        } catch (Exception e) {
            e.printStackTrace();
            notifyMessage.setData(false);
        }
        EventManager.getLibraryEvent().post(notifyMessage);
    }

    public void write_data(String data) {
        characteristic_write.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        characteristic_write.setValue(data);
        boolean writeCharacteristic = bluetoothGatt.writeCharacteristic(characteristic_write);
    }

    public void write_data(byte by[]) {
        characteristic_write.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        characteristic_write.setValue(by);
        boolean writeCharacteristic = bluetoothGatt.writeCharacteristic(characteristic_write);
    }

    public void read_data(int code) {
        this.code = code;
        bluetoothGatt.readCharacteristic(characteristic_read);
    }

    /**
     * Notify监听数据
     *
     * @param gatt
     * @param characteristic
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        String data = HexUtils.bytesToHexString(characteristic.getValue()); // 将字节转化为String字符串
        NotifyMessage notifyMessage = new NotifyMessage();
        notifyMessage.setCode(CodeUtils.SERVICE_ONNOTIFY);
        notifyMessage.setData(data);
        notifyMessage.setTag(tag);
        EventManager.getLibraryEvent().post(notifyMessage);
    }

    /**
     * 写出数据到设备之后 会回调该方法
     *
     * @param gatt
     * @param characteristic
     * @param status
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        NotifyMessage notifyMessag = new NotifyMessage();
        notifyMessag.setCode(CodeUtils.SERVICE_ONWRITE);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            notifyMessag.setData(true);
        } else {
            notifyMessag.setData(false);
        }
        EventManager.getLibraryEvent().post(notifyMessag);
    }

    /**
     * 主动读取通道当中的数据回调
     *
     * @param gatt
     * @param characteristic
     * @param status
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        NotifyMessage notifyMessage = new NotifyMessage();
        notifyMessage.setCode(CodeUtils.SERVICE_ONREAD);
        switch (code) {
            case CodeUtils.SERVICE_READ_DATA_BYTEARRAY:
                notifyMessage.setData(characteristic.getValue());
                break;
            case CodeUtils.SERVICE_READ_DATA_HEX2STR:
                notifyMessage.setData(HexUtils.bytesToHexString(characteristic.getValue()));
                break;
            case CodeUtils.SERVICE_READ_DATA:
                notifyMessage.setData(characteristic.getStringValue(0));
                break;
        }
        EventManager.getLibraryEvent().post(notifyMessage);
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        tag = null;
        if (bluetoothGatt != null) {
            bluetoothGatt.discoverServices();
            bluetoothGatt.disconnect();
        }
    }

    public BluetoothDevice getDevice() {
        return
                bluetoothGatt.getDevice();
    }
}
