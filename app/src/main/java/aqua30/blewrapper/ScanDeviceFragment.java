package aqua30.blewrapper;

import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import aqua.blewrapper.contracts.BluetoothManager;
import aqua.blewrapper.contracts.BluetoothViewContract;
import aqua.blewrapper.helper.BluetoothController;

import static aqua.blewrapper.helper.BluetoothController.log;


/**
 * Created by Saurabh on 03-01-2018.
 *
 * This dialog start scan for 10 seconds and list all the available BLE devices.
 * Selecting any device would initiate callback for the parent activity/fragment
 * and connection can be performed.
 *
 */

public class ScanDeviceFragment extends DialogFragment implements BluetoothViewContract.DiscoveryCallbacks {

    private ListView listView;
    private Button scanButton;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothManager bluetoothManager;
    private ScanDeviceListener scanDeviceListener;

    public void setScanDeviceListener(ScanDeviceListener scanDeviceListener) {
        this.scanDeviceListener = scanDeviceListener;
    }

    public interface ScanDeviceListener {
        void onDeviceSelected(BluetoothDevice device);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setDialog();
        View view = inflater.inflate(R.layout.fg_scan, container, false);
        listView = view.findViewById(R.id.scan_list);
        scanButton = view.findViewById(R.id.scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanButton.setText("Scanning..");
                bluetoothManager.scanDevices();
            }
        });
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mLeDeviceListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothManager.stopScanning();
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                log("selected device: " + device.getName());
                if (device == null) return;
                if (scanDeviceListener != null) {
                    scanDeviceListener.onDeviceSelected(device);
                }
                dismiss();
            }
        });

        bluetoothManager = new BluetoothController(getActivity());
        bluetoothManager.setDiscoveryCallbacks(this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDeviceDiscovered(final BluetoothDevice device) {
        if (getActivity() != null)
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
    }

    @Override
    public void onDeviceDiscoveryStopped() {
        scanButton.setText("Scan devices");
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = getActivity().getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private void setDialog(){
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getDialog().setCanceledOnTouchOutside(true);
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                getDialog().dismiss();
                return false;
            }
        });
    }
}
