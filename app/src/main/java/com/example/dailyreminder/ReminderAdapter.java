package com.example.dailyreminder;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Shows each reminder as a card in the RecyclerView on the main screen. */
public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.Holder> {

    public interface Listener {
        void onEdit(Reminder r);
        void onDelete(Reminder r);
        void onCompletedChanged(Reminder r, boolean completed);
    }

    private final Listener listener;
    private List<Reminder> items = new ArrayList<>();

    public ReminderAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Reminder> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Reminder r = items.get(position);
        h.title.setText(r.title);
        h.description.setText(r.description);
        h.description.setVisibility(r.description == null || r.description.isEmpty() ? View.GONE : View.VISIBLE);
        h.time.setText(r.getTimeText());
        h.days.setText(r.getDaysText() + (r.soundUri != null ? "  •  custom sound" : ""));

        // Completed reminders are crossed out and faded.
        if (r.completed) {
            h.title.setPaintFlags(h.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            h.title.setPaintFlags(h.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        h.itemView.setAlpha(r.completed ? 0.55f : 1f);

        h.done.setOnCheckedChangeListener(null);
        h.done.setChecked(r.completed);
        h.done.setOnCheckedChangeListener((b, checked) -> listener.onCompletedChanged(r, checked));

        h.itemView.setOnClickListener(v -> listener.onEdit(r));
        h.edit.setOnClickListener(v -> listener.onEdit(r));
        h.delete.setOnClickListener(v -> listener.onDelete(r));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title, description, time, days;
        final CheckBox done;
        final ImageButton edit, delete;

        Holder(View v) {
            super(v);
            title = v.findViewById(R.id.textTitle);
            description = v.findViewById(R.id.textDescription);
            time = v.findViewById(R.id.textTime);
            days = v.findViewById(R.id.textDays);
            done = v.findViewById(R.id.checkDone);
            edit = v.findViewById(R.id.buttonEdit);
            delete = v.findViewById(R.id.buttonDelete);
        }
    }
}
