package com.example.bluepos.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.example.bluepos.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DebtAdapter extends RecyclerView.Adapter<DebtAdapter.DebtViewHolder> {

    private List<Debt> debts;
    private OnDebtActionListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnDebtActionListener {
        void onPay(Debt debt);
        void onDelete(Debt debt);
        void onUndo(Debt debt);
    }

    public DebtAdapter(List<Debt> debts, OnDebtActionListener listener) {
        this.debts = debts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DebtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_debt, parent, false);
        return new DebtViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DebtViewHolder holder, int position) {
        Debt debt = debts.get(position);
        holder.tvCustomerName.setText(debt.customerName);
        holder.tvProductName.setText(debt.productName + " x " + debt.quantity);
        holder.tvDebtNote.setText(debt.note);
        holder.tvDebtAmount.setText(String.format("Amount: ₱%.2f", debt.amount));
        holder.tvDebtDate.setText("Date: " + dateFormat.format(new Date(debt.timestamp)));
        holder.tvDebtStatus.setText(debt.status.toUpperCase());

        if ("Unpaid".equalsIgnoreCase(debt.status)) {
            holder.tvDebtStatus.setBackgroundResource(R.drawable.bg_status_pending);
            holder.btnMarkPaid.setVisibility(View.VISIBLE);
            holder.btnUndoDebt.setVisibility(View.GONE);
        } else {
            holder.tvDebtStatus.setBackgroundResource(R.drawable.bg_status_completed);
            holder.btnMarkPaid.setVisibility(View.GONE);
            holder.btnUndoDebt.setVisibility(View.VISIBLE);
        }

        holder.btnMarkPaid.setOnClickListener(v -> listener.onPay(debt));
        holder.btnDeleteDebt.setOnClickListener(v -> listener.onDelete(debt));
        holder.btnUndoDebt.setOnClickListener(v -> listener.onUndo(debt));
    }

    @Override
    public int getItemCount() {
        return debts.size();
    }

    public void setDebts(List<Debt> debts) {
        this.debts = debts;
        notifyDataSetChanged();
    }

    static class DebtViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvProductName, tvDebtNote, tvDebtAmount, tvDebtDate, tvDebtStatus;
        MaterialButton btnMarkPaid, btnDeleteDebt, btnUndoDebt;

        public DebtViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvDebtNote = itemView.findViewById(R.id.tvDebtNote);
            tvDebtAmount = itemView.findViewById(R.id.tvDebtAmount);
            tvDebtDate = itemView.findViewById(R.id.tvDebtDate);
            tvDebtStatus = itemView.findViewById(R.id.tvDebtStatus);
            btnMarkPaid = itemView.findViewById(R.id.btnMarkPaid);
            btnDeleteDebt = itemView.findViewById(R.id.btnDeleteDebt);
            btnUndoDebt = itemView.findViewById(R.id.btnUndoDebt);
        }
    }
}
