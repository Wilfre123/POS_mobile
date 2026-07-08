package com.example.bluepos;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bluepos.pos.Product;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpiringProductAdapter extends RecyclerView.Adapter<ExpiringProductAdapter.ViewHolder> {

    private List<Product> products;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public ExpiringProductAdapter(List<Product> products) {
        this.products = products;
    }

    public void updateList(List<Product> newList) {
        this.products = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expiring_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.tvName.setText(product.name);
        
        if (product.expirationDate != null) {
            String dateStr = sdf.format(new Date(product.expirationDate));
            holder.tvExpDate.setText("Expires on: " + dateStr);

            long currentTime = System.currentTimeMillis();
            long diff = product.expirationDate - currentTime;
            long daysLeft = diff / (1000 * 60 * 60 * 24);

            if (diff < 0) {
                holder.indicator.setBackgroundColor(Color.RED);
                holder.tvDaysLeft.setText("EXPIRED");
                holder.tvDaysLeft.setTextColor(Color.RED);
            } else if (daysLeft <= 7) {
                holder.indicator.setBackgroundColor(Color.YELLOW);
                holder.tvDaysLeft.setText(daysLeft + " Days Left");
                holder.tvDaysLeft.setTextColor(Color.parseColor("#FFA500")); // Orange/Yellow
            } else {
                holder.indicator.setBackgroundColor(Color.GREEN);
                holder.tvDaysLeft.setText(daysLeft + " Days Left");
                holder.tvDaysLeft.setTextColor(Color.GREEN);
            }
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvExpDate, tvDaysLeft;
        View indicator;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvExpDate = itemView.findViewById(R.id.tvExpirationDate);
            tvDaysLeft = itemView.findViewById(R.id.tvDaysLeft);
            indicator = itemView.findViewById(R.id.indicatorColor);
        }
    }
}