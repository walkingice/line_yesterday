package cc.jchu.naver.line.yesterday.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.asImage
import coil3.load
import cc.jchu.naver.line.yesterday.R
import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.FeedItem
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.domain.SpaceFlightItem
import cc.jchu.naver.line.yesterday.data.domain.displayName
import cc.jchu.naver.line.yesterday.databinding.ItemDummyJsonBinding
import cc.jchu.naver.line.yesterday.databinding.ItemFeedFooterBinding
import cc.jchu.naver.line.yesterday.databinding.ItemSpaceFlightBinding
import java.util.concurrent.Executor

class FeedAdapter(
    private val onItemClick: (FeedItem) -> Unit,
    private val onFooterClick: () -> Unit,
    backgroundExecutor: Executor? = null,
) : ListAdapter<FeedAdapter.Row, RecyclerView.ViewHolder>(
    AsyncDifferConfig.Builder(DIFF_CALLBACK).apply {
        backgroundExecutor?.let(::setBackgroundThreadExecutor)
    }.build(),
) {

    init {
        setHasStableIds(true)
    }

    fun submitFeed(
        items: List<FeedItem>,
        footerState: FeedFooterState,
        onCommitted: () -> Unit = {},
    ) {
        submitList(items.map(Row::Item) + Row.Footer(footerState), onCommitted)
    }

    override fun getItemViewType(position: Int): Int = when (val row = getItem(position)) {
        is Row.Item -> when (row.item.source) {
            FeedSource.DUMMY_JSON -> VIEW_TYPE_DUMMY_JSON
            FeedSource.SPACE_FLIGHT -> VIEW_TYPE_SPACE_FLIGHT
        }
        is Row.Footer -> VIEW_TYPE_FOOTER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_DUMMY_JSON -> DummyJsonViewHolder(
                ItemDummyJsonBinding.inflate(inflater, parent, false),
                onItemClick,
            )
            VIEW_TYPE_SPACE_FLIGHT -> SpaceFlightViewHolder(
                ItemSpaceFlightBinding.inflate(inflater, parent, false),
                onItemClick,
            )
            VIEW_TYPE_FOOTER -> FooterViewHolder(
                ItemFeedFooterBinding.inflate(inflater, parent, false),
                onFooterClick,
            )
            else -> error("Unknown FeedAdapter view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DummyJsonViewHolder -> holder.bind((getItem(position) as Row.Item).item)
            is SpaceFlightViewHolder -> holder.bind((getItem(position) as Row.Item).item)
            is FooterViewHolder -> holder.bind((getItem(position) as Row.Footer).state)
        }
    }

    override fun getItemId(position: Int): Long = when (val row = getItem(position)) {
        is Row.Item -> stableId(row.item)
        is Row.Footer -> FOOTER_ID
    }

    sealed interface Row {
        data class Item(val item: FeedItem) : Row
        data class Footer(val state: FeedFooterState) : Row
    }

    private class DummyJsonViewHolder(
        private val binding: ItemDummyJsonBinding,
        private val onClick: (FeedItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FeedItem) {
            val dummyItem = item as DummyJsonItem
            binding.image.load(dummyItem.imgUrl) {
                placeholder(binding.root.context.placeholderImage())
                error(binding.root.context.placeholderImage())
            }
            binding.title.text = dummyItem.title
            binding.category.text = dummyItem.category
            binding.time.text = binding.root.context.getString(
                R.string.item_time,
                dummyItem.time.ifBlank {
                    binding.root.context.getString(R.string.item_time_not_available)
                },
            )
            binding.source.text = binding.root.context.getString(
                R.string.item_source,
                dummyItem.source.displayName(),
            )
            binding.root.setOnClickListener { onClick(dummyItem) }
        }
    }

    private class SpaceFlightViewHolder(
        private val binding: ItemSpaceFlightBinding,
        private val onClick: (FeedItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FeedItem) {
            val spaceItem = item as SpaceFlightItem
            binding.image.load(spaceItem.imgUrl) {
                placeholder(binding.root.context.placeholderImage())
                error(binding.root.context.placeholderImage())
            }
            binding.title.text = spaceItem.title
            binding.description.text = spaceItem.description
            binding.time.text = binding.root.context.getString(
                R.string.item_time,
                spaceItem.time.ifBlank {
                    binding.root.context.getString(R.string.item_time_not_available)
                },
            )
            binding.source.text = binding.root.context.getString(
                R.string.item_source,
                spaceItem.source.displayName(),
            )
            binding.root.setOnClickListener { onClick(spaceItem) }
        }
    }

    private class FooterViewHolder(
        private val binding: ItemFeedFooterBinding,
        private val onClick: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(state: FeedFooterState) {
            binding.footerButton.text = state.buttonText()
            binding.footerButton.isEnabled = state != FeedFooterState.Loading &&
                state != FeedFooterState.NoMoreItems
            binding.footerButton.setOnClickListener {
                if (binding.footerButton.isEnabled) onClick()
            }
        }
    }

    private companion object {
        const val VIEW_TYPE_DUMMY_JSON = 1
        const val VIEW_TYPE_SPACE_FLIGHT = 2
        const val VIEW_TYPE_FOOTER = 3
        const val FOOTER_ID = Long.MIN_VALUE

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Row>() {
            override fun areItemsTheSame(oldItem: Row, newItem: Row): Boolean = when {
                oldItem is Row.Item && newItem is Row.Item ->
                    oldItem.item.source == newItem.item.source && oldItem.item.id == newItem.item.id
                oldItem is Row.Footer && newItem is Row.Footer -> true
                else -> false
            }

            override fun areContentsTheSame(oldItem: Row, newItem: Row): Boolean =
                oldItem == newItem
        }

        fun stableId(item: FeedItem): Long =
            "${item.source.name}:${item.id}".hashCode().toLong()

        fun FeedFooterState.buttonText(): String = when (this) {
            FeedFooterState.Ready -> "Load more"
            FeedFooterState.Loading -> "Loading..."
            FeedFooterState.NoMoreItems -> "No more items"
            FeedFooterState.Error -> "Retry"
            FeedFooterState.Offline -> "Retry while online"
        }

        fun android.content.Context.placeholderImage() =
            getDrawable(cc.jchu.naver.line.yesterday.R.drawable.ic_launcher_foreground)?.asImage()
    }
}
