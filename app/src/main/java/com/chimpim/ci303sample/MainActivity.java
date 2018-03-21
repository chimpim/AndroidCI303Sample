package com.chimpim.ci303sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chimpim.rfidci303.CI303Reader;
import com.chimpim.rfidci303.CI303ReaderSupport;
import com.chimpim.rfidci303.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android_serialport_api.ArgEnums;
import android_serialport_api.ISerialPort;
import android_serialport_api.SerialPort;

public class MainActivity extends Activity {
    private Button mBtnConnect;
    private Button mBtnStartRead;
    private Button mBtnStopRead;
    private TextView mTvLog;
    private EditText mEtSerialPort;
    private CI303Reader mCI303Reader;

    private volatile boolean isStartRead = false;

    private static final ExecutorService IO_THREAD_POOL = Executors.newCachedThreadPool();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        final String port = mEtSerialPort.getText().toString();
        mBtnConnect.setOnClickListener(v -> {
            //
            runOnIoThread(() -> {
                if (mCI303Reader != null) {
                    mCI303Reader.disconnect();
                }
                CI303Reader.ConnectionAdapter connectionAdapter = new CI303Reader.ConnectionAdapter() {
                    ISerialPort serialPort;

                    @Override
                    public void connect() throws Exception {
                        serialPort = SerialPort.open(port, 9600,
                                ArgEnums.DataBit.DATA_BIT_8,
                                ArgEnums.CheckBit.CHECK_BIT_NONE,
                                ArgEnums.StopBit.STOP_BIT_1,
                                0);
                    }

                    @Override
                    public boolean hasConnected() {
                        return serialPort != null && serialPort.isOpen();
                    }

                    @Override
                    public OutputStream getOutputStream() throws IOException {
                        if (serialPort == null) return null;
                        return serialPort.getOutputStream();
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        if (serialPort == null) return null;
                        return serialPort.getInputStream();
                    }

                    @Override
                    public void disconnect() {
                        if (serialPort != null) {
                            try {
                                serialPort.shutdown();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            serialPort = null;
                        }
                    }
                };
                mCI303Reader = CI303ReaderSupport.connect(connectionAdapter);
                if (mCI303Reader == null) {
                    runOnUiThread(() -> Toast.makeText(this, "连接失败(" + port + ")", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "连接成功(" + port + ")", Toast.LENGTH_SHORT).show());
                }
            });
        });
        mBtnStartRead.setOnClickListener(v -> {
            isStartRead = true;
            runOnIoThread(() -> {
                while (isStartRead) {
                    try {
                        Tag[] tags = CI303ReaderSupport.gen2MultiTagIdentify(mCI303Reader, 1000);
                        runOnUiThread(() -> mTvLog.setText(Arrays.toString(tags) + "\n" + mTvLog.getText()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> mTvLog.setText("读卡异常:" + e.toString() + "\n" + mTvLog.getText()));
                        SystemClock.sleep(1000);
                    }
                }
            });
        });
        mBtnStopRead.setOnClickListener(v -> isStartRead = false);
    }

    private void initView() {
        mBtnConnect = findViewById(R.id.btn_connect);
        mBtnStartRead = findViewById(R.id.btn_start_read);
        mBtnStopRead = findViewById(R.id.btn_stop_read);
        mTvLog = findViewById(R.id.tv_log);
        mEtSerialPort = findViewById(R.id.et_serial_port);
    }

    private void runOnIoThread(@NonNull Runnable action) {
        IO_THREAD_POOL.execute(action);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCI303Reader != null) {
            mCI303Reader.disconnect();
        }
    }
}
