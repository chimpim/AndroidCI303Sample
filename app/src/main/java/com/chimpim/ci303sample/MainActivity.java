package com.chimpim.ci303sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chimpim.reader.ci303.CI303Reader;
import com.chimpim.reader.ci303.CI303ReaderSupport;
import com.chimpim.reader.ci303.TagData;
import com.chimpim.serialport.AndroidSerialPort;
import com.chimpim.serialport.SerialPortException;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private Button mBtnConnect;
    private Button mBtnStartRead;
    private Button mBtnStopRead;
    private TextView mTvLog;
    private EditText mEtSerialPort;
    private CI303Reader mCI303Reader;

    private AtomicBoolean isStartRead = new AtomicBoolean(false);

    private static final ExecutorService IO_THREAD_POOL = Executors.newCachedThreadPool();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        final String port = mEtSerialPort.getText().toString();
        mCI303Reader = new CI303Reader(new AndroidSerialPort(), port, 9600);
        mBtnConnect.setOnClickListener(v -> {
            //
            runOnIoThread(() -> {
                try {
                    mCI303Reader.connect();
                    runOnUiThread(() -> Toast.makeText(this, "连接成功(" + port + ")", Toast.LENGTH_SHORT).show());
                } catch (SerialPortException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "连接失败(" + e + ")", Toast.LENGTH_SHORT).show());
                }
            });
        });
        mBtnStartRead.setOnClickListener(v -> runOnIoThread(() -> {
            isStartRead.set(true);
            while (isStartRead.get()) {
                try {
                    TagData[] tagData = CI303ReaderSupport.gen2MultiTagIdentify(mCI303Reader, 1000);
                    runOnUiThread(() -> mTvLog.setText(Arrays.toString(tagData) + "\n" + mTvLog.getText()));
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> mTvLog.setText("读卡异常:" + e.toString() + "\n" + mTvLog.getText()));
                    SystemClock.sleep(1000);
                }
            }
        }));
        mBtnStopRead.setOnClickListener(v -> isStartRead.set(false));
    }

    private void initView() {
        mBtnConnect = findViewById(R.id.btn_connect);
        mBtnStartRead = findViewById(R.id.btn_start_read);
        mBtnStopRead = findViewById(R.id.btn_stop_read);
        mTvLog = findViewById(R.id.tv_log);
        mEtSerialPort = findViewById(R.id.et_serial_port);
    }

    private void runOnIoThread(Runnable action) {
        if (action == null) throw new NullPointerException("action no null");
        IO_THREAD_POOL.execute(action);
    }
}
