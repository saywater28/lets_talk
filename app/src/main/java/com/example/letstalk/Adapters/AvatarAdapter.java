package com.example.letstalk.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.letstalk.R;

public class AvatarAdapter extends ArrayAdapter<Integer> {

        private final Context context;
        private final Integer[] avatars;

        public AvatarAdapter(Context context, Integer[] avatars) {
            super(context, 0, avatars);
            this.context = context;
            this.avatars = avatars;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context)
                        .inflate(R.layout.item_avatar, parent, false);
            }

            ImageView avatarImage = convertView.findViewById(R.id.avatarImage);
            avatarImage.setImageResource(avatars[position]);

            return convertView;
        }
}

