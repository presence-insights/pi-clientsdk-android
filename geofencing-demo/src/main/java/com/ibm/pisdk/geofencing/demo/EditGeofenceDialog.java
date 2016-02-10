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
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.ibm.pisdk.geofencing.PIGeofence;
import com.ibm.pisdk.geofencing.rest.PIRequestCallback;
import com.ibm.pisdk.geofencing.rest.PIRequestError;

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
    private int dialogMode = MODE_NEW;
    private MapsActivity.GeofenceInfo fenceInfo;
    private PIGeofence fence;
    private int[] allRadius = {
        100, 200, 250, 300, 350, 400, 450, 500, 600, 700, 800, 900, 1000, 1250, 1500, 1750, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 10000
    };
    private int radius = allRadius[0];

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
        final EditText descView = (EditText) view.findViewById(R.id.geofence_desc);
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
        builder.setView(view);
        if (dialogMode == MODE_NEW) {
            builder.setPositiveButton(R.string.add_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    String name = nameView.getText().toString();
                    String desc = descView.getText().toString();
                    PIGeofence fence = new PIGeofence(UUID.randomUUID().toString(), name, desc, position.latitude, position.longitude, radius);
                    mapsActivity.service.registerGeofence(fence, new PIRequestCallback<PIGeofence>() {
                        @Override
                        public void onSuccess(PIGeofence fence) {
                            fence.save();
                            List<PIGeofence> list = new ArrayList<>();
                            mapsActivity.service.monitorGeofences(list);
                            mapsActivity.refreshGeofenceInfo(fence, false);
                        }

                        @Override
                        public void onError(PIRequestError error) {
                        }
                    });
                    performCommonActions(dialog, null);
                }
            });
        } else if (dialogMode == MODE_UPDATE_DELETE) {
            builder.setPositiveButton(R.string.update_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    fence.setName(nameView.getText().toString());
                    fence.setDescription(descView.getText().toString());
                    fence.setRadius(radius);
                    fence.setLatitude(position.latitude);
                    fence.setLongitude(position.longitude);
                    List<PIGeofence> list = new ArrayList<>();
                    list.add(fence);
                    mapsActivity.service.unmonitorGeofences(list);
                    mapsActivity.service.monitorGeofences(list);
                    mapsActivity.removeGeofence(fence);
                    mapsActivity.refreshGeofenceInfo(fence, (fenceInfo != null) && fenceInfo.active);
                    fence.save();
                    mapsActivity.refreshGeofenceInfo(fence, (fenceInfo != null) && fenceInfo.active);
                    performCommonActions(dialog, null);
                }
            });
            builder.setNeutralButton(R.string.delete_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    fence.delete();
                    List<PIGeofence> list = new ArrayList<>();
                    list.add(fence);
                    mapsActivity.service.deleteGeofences(list);
                    mapsActivity.service.unmonitorGeofences(list);
                    mapsActivity.removeGeofence(fence);
                    performCommonActions(dialog, null);
                }
            });
        }
        builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                performCommonActions(dialog, null);
            }
        });
        final Button locationButton = (Button) view.findViewById(R.id.change_location);
        final Dialog dlg = builder.create();
        if (fence != null) {
            radius = (int) fence.getRadius();
            seekBar.setProgress(progressFromRadius(radius));
            radiusValueView.setText(String.format("%,d m", (int) fence.getRadius()));
            nameView.setText(fence.getName());
            descView.setText(fence.getDescription());
            locationButton.setEnabled(true);
            locationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    performCommonActions(dlg, fenceInfo);
                    mapsActivity.switchMode();
                }
            });
        } else {
            locationButton.setEnabled(false);
            locationButton.setVisibility(View.GONE);
        }
        return dlg;
    }

    private void performCommonActions(DialogInterface dialog, MapsActivity.GeofenceInfo editedInfo) {
        mapsActivity.editedInfo = editedInfo;
        mapsActivity.refreshCurrentLocation();
        dialog.cancel();
    }

    public void customInit(MapsActivity mapsActivity, int dialogMode, MapsActivity.GeofenceInfo fenceInfo) {
        this.mapsActivity = mapsActivity;
        this.dialogMode = dialogMode;
        this.fenceInfo = fenceInfo;
        if (fenceInfo == null) {
            this.position = mapsActivity.googleMap.getCameraPosition().target;
        } else {
            fence = mapsActivity.getGeofenceManager().getGeofence(fenceInfo.uuid);
            if ((dialogMode == MODE_UPDATE_DELETE)  && (mapsActivity.editedInfo != null)) {
                this.position = mapsActivity.googleMap.getCameraPosition().target;
            } else {
                this.position = new LatLng(fence.getLatitude(), fence.getLongitude());
            }
        }
    }

    /**
     * Compute the seek bar position from the specified geofence's radius.
     */
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

    /**
     * Compute the radius value from the seek bar position.
     */
    private int radiusFromProgress(int progress) {
        return allRadius[progress];
    }
}
