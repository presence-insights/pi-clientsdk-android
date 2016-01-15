/**
 * Copyright (c) 2015-2016 IBM Corporation. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.pisdk.geofencing.demo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.ibm.pisdk.geofencing.PIGeofence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 */
public class EditGeofenceDialog extends DialogFragment {
    static final int MODE_NEW = 1;
    static final int MODE_UPDATE_DELETE = 2;
    private LatLng position;
    private MapsActivity mapsActivity;
    private int radius = 100;
    private int dialogMode = MODE_NEW;
    private MapsActivity.GeofenceInfo fenceInfo;
    private PIGeofence fence;
    private int[] allRadius = {
        100, 200, 250, 300, 350, 400, 450, 500, 600, 700, 800, 900, 1000, 1250, 1500, 1750, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 10000
    };

    public EditGeofenceDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_geofence, null);
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.radius_seekbar_id);
        final TextView radiusValueView = (TextView) view.findViewById(R.id.radius_value);
        final EditText nameView = (EditText) view.findViewById(R.id.geofence_name);
        TextView positionView = (TextView) view.findViewById(R.id.fence_location);
        String text = String.format("Latitude: %.6f - Longitude: %.6f", position.latitude, position.longitude);
        positionView.setText(text);
        seekBar.setMax(allRadius.length - 1);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                radius = radiusFromProgress(progress);
                radiusValueView.setText(String.format("%,d m", radius));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        if (fence != null) {
            seekBar.setProgress(progressFromRadius((int) fence.getRadius()));
            radiusValueView.setText(String.format("%,d m", (int) fence.getRadius()));
            nameView.setText(fence.getName());
        }
        builder.setView(view);
        if (dialogMode == MODE_NEW) {
            builder.setPositiveButton(R.string.add_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    String name = nameView.getText().toString();
                    dialog.cancel();
                    PIGeofence fence = new PIGeofence(UUID.randomUUID().toString(), name, position.latitude, position.longitude, radius, null, null, null);
                    fence.save();
                    List<PIGeofence> list = new ArrayList<>();
                    list.add(fence);
                    mapsActivity.service.addGeofences(list);
                    mapsActivity.refreshGeofenceInfo(fence, false);
                    mapsActivity.refreshCurrentLocation();
                }
            });
        } else if (dialogMode == MODE_UPDATE_DELETE) {
            // todo: handle when the radius has changed, including registering the fence anew in the location API
            // for now we just disable it
            seekBar.setEnabled(false);
            builder.setPositiveButton(R.string.update_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    String name = nameView.getText().toString();
                    fence.setName(name);
                    fence.save();
                    dialog.cancel();
                    mapsActivity.refreshGeofenceInfo(fence, (fenceInfo != null) && fenceInfo.active);
                    mapsActivity.refreshCurrentLocation();
                }
            });
            builder.setNeutralButton(R.string.delete_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    fence.delete();
                    List<PIGeofence> list = new ArrayList<>();
                    list.add(fence);
                    mapsActivity.service.removeGeofences(list);
                    mapsActivity.removeGeofence(fence);
                    mapsActivity.refreshCurrentLocation();
                }
            });
        }
        builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                mapsActivity.refreshCurrentLocation();
            }
        });
        return builder.create();
    }

    public void customInit(MapsActivity mapsActivity, int dialogMode, MapsActivity.GeofenceInfo fenceInfo) {
        this.mapsActivity = mapsActivity;
        this.dialogMode = dialogMode;
        this.fenceInfo = fenceInfo;
        if (fenceInfo == null) {
            this.position = mapsActivity.googleMap.getCameraPosition().target;
        } else {
            fence = mapsActivity.getGeofenceManager().getGeofence(fenceInfo.uuid);
            this.position = new LatLng(fence.getLatitude(), fence.getLongitude());
        }
    }

    private int progressFromRadius(int radius) {
        int minDiff = Integer.MAX_VALUE;
        int minProgress = -1;
        for (int i=0; i<allRadius.length; i++) {
            int diff = Math.abs(radius - allRadius[i]);
            if (diff < minDiff) {
                minProgress = i;
                minDiff = diff;
            }
        }
        return minProgress;
    }

    private int radiusFromProgress(int progress) {
        return allRadius[progress];
    }
}
