package com.example.smartpantry.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantry.R;
import com.example.smartpantry.data.PantryDbHelper.PantryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Custom adapter that binds a list of PantryItem objects (read from SQLite)
 * to the item_pantry.xml row layout.
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.ViewHolder> {

    /** The Activity implements this so the adapter stays free of database code. */
    public interface OnItemActionListener {
        void onEdit(PantryItem item);
        void onDelete(PantryItem item);
    }

    private static final int EXPIRING_SOON_DAYS = 3;

    private final List<PantryItem> items = new ArrayList<>();
    private final OnItemActionListener listener;

    public PantryAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    /** Replaces the displayed data (called after every database change). */
    public void setItems(List<PantryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PantryItem item = items.get(position);

        holder.tvName.setText(item.name);
        holder.tvQuantity.setText(formatQuantity(item.quantity) + " " + item.unit);

        if (item.expiryDate == null || item.expiryDate.isEmpty()) {
            holder.tvExpiry.setText("No expiry date");
            holder.tvExpiry.setTextColor(Color.GRAY);
        } else {
            long daysLeft = daysUntil(item.expiryDate);
            if (daysLeft < 0) {
                holder.tvExpiry.setText("Expired: " + item.expiryDate);
                holder.tvExpiry.setTextColor(Color.RED);
            } else if (daysLeft <= EXPIRING_SOON_DAYS) {
                holder.tvExpiry.setText("Expires soon: " + item.expiryDate);
                holder.tvExpiry.setTextColor(Color.parseColor("#E65100")); // orange
            } else {
                holder.tvExpiry.setText("Expires: " + item.expiryDate);
                holder.tvExpiry.setTextColor(Color.GRAY);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onEdit(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** 300.0 -> "300", 1.5 -> "1.5" */
    private String formatQuantity(double q) {
        return q == Math.rint(q) ? String.valueOf((long) q) : String.valueOf(q);
    }

    /** Whole days from today until the given yyyy-MM-dd date (negative if past). */
    private long daysUntil(String dateText) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date expiry = fmt.parse(dateText);
            Date today = fmt.parse(fmt.format(new Date())); // strip the time of day
            if (expiry == null || today == null) return Long.MAX_VALUE;
            return TimeUnit.MILLISECONDS.toDays(expiry.getTime() - today.getTime());
        } catch (java.text.ParseException e) {
            return Long.MAX_VALUE; // unparseable: treat as "not expiring"
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvQuantity, tvExpiry;
        final ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvQuantity = itemView.findViewById(R.id.tvItemQuantity);
            tvExpiry = itemView.findViewById(R.id.tvItemExpiry);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
