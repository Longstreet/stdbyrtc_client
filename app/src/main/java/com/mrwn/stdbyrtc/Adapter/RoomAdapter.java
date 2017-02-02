package com.mrwn.stdbyrtc.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mrwn.stdbyrtc.MainActivity;
import com.mrwn.stdbyrtc.Model.Room;
import com.mrwn.stdbyrtc.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Marouane on 12-09-2016
 */
public class RoomAdapter extends ArrayAdapter<Room> {
    private final Context context;
    private LayoutInflater inflater;
    private List<Room> values;
    private Map<String, Room> rooms;
    private ArrayList<Room> items;
    private ArrayList<Room> itemsAll;
    private ArrayList<Room> suggestions;

    public RoomAdapter(Context context, ArrayList<Room> values) {
        super(context, R.layout.room_row_layout, android.R.id.text1, values);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.values = values;
        this.rooms = new HashMap<>();
        this.items = values;
        this.itemsAll = (ArrayList<Room>) items.clone();
        this.suggestions = new ArrayList<>();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Room roomItem = this.values.get(position);
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.room_row_layout, parent, false);
            holder.roomName = (TextView) convertView.findViewById(R.id.room_name);
            holder.roomOwner = (TextView) convertView.findViewById(R.id.history_status);
            holder.roomTime = (TextView) convertView.findViewById(R.id.room_time);
            holder.roomJoinBtn = (ImageButton) convertView.findViewById(R.id.room_join);
            holder.browserBtn = (ImageButton) convertView.findViewById(R.id.room_call_browser);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.roomName.setText(roomItem.getRoomName());
        holder.roomOwner.setText(roomItem.getRoomId());
        if (roomItem.getRoomStatus().equals("Online")) {
            holder.roomOwner.setTextColor(Color.GREEN);
        } else {
            holder.roomOwner.setTextColor(Color.RED);
        }
        holder.roomOwner.setText(roomItem.getRoomStatus());
        holder.roomJoinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.roomJoinBtn.setEnabled(false);
                ((MainActivity) v.getContext()).makeCall(holder.roomTime.getText().toString(), holder.roomOwner.getText().toString());
            }
        });
//        holder.browserBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ((MainActivity) v.getContext()).makeBrowserCall(holder.id.getText().toString(), holder.status.getText().toString());
//            }
//        });
        holder.histItem = roomItem;
        return convertView;
    }

    @Override
    public int getCount() {
        return this.values.size();
    }

    public void removeButton(int loc) {
        this.values.remove(loc);
        notifyDataSetChanged();
    }

    class ViewHolder {
        TextView roomName;
        TextView roomOwner;
        TextView roomTime;
        ImageButton roomJoinBtn;
        ImageButton browserBtn;
        Room histItem;
    }
}

