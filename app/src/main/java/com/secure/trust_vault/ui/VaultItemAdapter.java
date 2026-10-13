package com.secure.trust_vault.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.secure.trust_vault.R;
import com.secure.trust_vault.model.VaultItem;

/**
 * VaultItemAdapter
 * RecyclerView ListAdapter displaying encrypted vault items with smooth animations,
 * category badges, one-tap clipboard copy, and item click callbacks.
 */
public class VaultItemAdapter extends ListAdapter<VaultItem, VaultItemAdapter.VaultViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(VaultItem item);
        void onDeleteClick(VaultItem item);
        void onCopyClick(VaultItem item);
    }

    private final OnItemClickListener listener;

    public VaultItemAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<VaultItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<VaultItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull VaultItem oldItem, @NonNull VaultItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull VaultItem oldItem, @NonNull VaultItem newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getCategory().equals(newItem.getCategory()) &&
                    (oldItem.getUsername() != null && oldItem.getUsername().equals(newItem.getUsername()));
        }
    };

    @NonNull
    @Override
    public VaultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vault_card, parent, false);
        return new VaultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VaultViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class VaultViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout iconContainer;
        private final ImageView ivIcon;
        private final TextView tvTitle;
        private final TextView tvSubtitle;
        private final ImageView ivHasDocument;
        private final ImageButton btnCopy;
        private final ImageButton btnDelete;

        public VaultViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.icon_container);
            ivIcon = itemView.findViewById(R.id.iv_item_icon);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvSubtitle = itemView.findViewById(R.id.tv_item_subtitle);
            ivHasDocument = itemView.findViewById(R.id.iv_has_document);
            btnCopy = itemView.findViewById(R.id.btn_quick_copy);
            btnDelete = itemView.findViewById(R.id.btn_delete_item);
        }

        public void bind(VaultItem item) {
            Context context = itemView.getContext();
            tvTitle.setText(item.getTitle());

            String sub = item.getUsername();
            if (sub == null || sub.trim().isEmpty()) {
                sub = item.getCategory().toLowerCase();
            }
            tvSubtitle.setText(sub);

            if (item.getDocumentPath() != null && !item.getDocumentPath().isEmpty()) {
                ivHasDocument.setVisibility(View.VISIBLE);
            } else {
                ivHasDocument.setVisibility(View.GONE);
            }

            // Configure category icon and styling
            int iconRes = R.drawable.ic_key;
            int tintColor = ContextCompat.getColor(context, R.color.cat_password);
            int bgTint = ContextCompat.getColor(context, R.color.cat_password_bg);

            switch (item.getCategory()) {
                case VaultItem.CAT_CARD:
                    iconRes = R.drawable.ic_credit_card;
                    tintColor = ContextCompat.getColor(context, R.color.cat_card);
                    bgTint = ContextCompat.getColor(context, R.color.cat_card_bg);
                    break;
                case VaultItem.CAT_NOTE:
                    iconRes = R.drawable.ic_note;
                    tintColor = ContextCompat.getColor(context, R.color.cat_note);
                    bgTint = ContextCompat.getColor(context, R.color.cat_note_bg);
                    break;
                case VaultItem.CAT_DOCUMENT:
                    iconRes = R.drawable.ic_document;
                    tintColor = ContextCompat.getColor(context, R.color.cat_doc);
                    bgTint = ContextCompat.getColor(context, R.color.cat_doc_bg);
                    break;
                case VaultItem.CAT_ID_CARD:
                    iconRes = R.drawable.ic_shield;
                    tintColor = ContextCompat.getColor(context, R.color.status_warning);
                    bgTint = 0x26F59E0B;
                    break;
                case VaultItem.CAT_PERSONAL_INFO:
                    iconRes = R.drawable.ic_info;
                    tintColor = ContextCompat.getColor(context, R.color.accent_cyan);
                    bgTint = 0x2606B6D4;
                    break;
            }

            ivIcon.setImageResource(iconRes);
            ivIcon.setImageTintList(ColorStateList.valueOf(tintColor));
            iconContainer.setBackgroundTintList(ColorStateList.valueOf(bgTint));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });

            btnCopy.setOnClickListener(v -> {
                if (listener != null) listener.onCopyClick(item);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(item);
            });
        }
    }
}
