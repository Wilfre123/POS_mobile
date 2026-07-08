package com.example.bluepos.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bluepos.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SaleAdapter extends RecyclerView.Adapter<SaleAdapter.SaleViewHolder> {

    private List<Sale> sales;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    public SaleAdapter(List<Sale> sales) {
        this.sales = sales;
    }

    public void updateList(List<Sale> newList) {
        this.sales = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SaleViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale, parent, false);
        return new SaleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SaleViewHolder holder, int position) {
        Sale sale = sales.get(position);
        holder.tvTotal.setText(String.format(Locale.US, "₱%.2f", sale.totalAmount));
        holder.tvDate.setText(dateFormat.format(new Date(sale.timestamp)));
        holder.tvItems.setText(sale.itemsSummary);
        holder.tvPayment.setText(String.format(Locale.US, "Paid: ₱%.2f | Change: ₱%.2f", sale.amountPaid, sale.change));
        holder.tvUser.setText("Sold by: " + (sale.userName != null ? sale.userName : "Unknown"));
    }

    @Override
    public int getItemCount() {
        return sales.size();
    }

    static class SaleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTotal, tvDate, tvItems, tvPayment, tvUser;

        public SaleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTotal = itemView.findViewById(R.id.tvSaleTotal);
            tvDate = itemView.findViewById(R.id.tvSaleDate);
            tvItems = itemView.findViewById(R.id.tvSaleItems);
            tvPayment = itemView.findViewById(R.id.tvSalePayment);
            tvUser = itemView.findViewById(R.id.tvSaleUser);
        }
    }
}
