package org.geogebra.web.full.gui.toolbarpanel;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.awt.GPoint;
import org.geogebra.common.gui.SetLabels;
import org.geogebra.common.gui.view.table.TableValuesDimensions;
import org.geogebra.common.gui.view.table.TableValuesListener;
import org.geogebra.common.gui.view.table.TableValuesModel;
import org.geogebra.common.gui.view.table.TableValuesView;
import org.geogebra.common.kernel.kernelND.GeoEvaluatable;
import org.geogebra.web.full.css.MaterialDesignResources;
import org.geogebra.web.full.gui.util.MyToggleButtonW;
import org.geogebra.web.html5.gui.util.NoDragImage;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.html5.util.CSSEvents;
import org.geogebra.web.html5.util.Dom;
import org.geogebra.web.html5.util.TableUtils;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;

/**
 * HTML representation of the Table of Values View.
 *
 * @author laszlo
 *
 */
public class TableValuesPanel extends FlowPanel implements SetLabels, TableValuesListener {

	// margin to align value cells to header - 3dot empty place
	private static final int VALUE_RIGHT_MARGIN = 36;
	// Extra padding for x column
	private static final int X_LEFT_PADDING = 16;
	private static final int TOOLBAR_HEADER_HEIGHT = 48;
	private static final int TABLE_HEADER_HEIGHT = 40;

	// minimum values comes from design
	private static final int MIN_COLUMN_WIDTH = 72;
	private static final int STRICT_ROW_HEIGHT = 40;

	/** view of table values */
	TableValuesView view;

	/** Template to create a cell */
	static final CellTemplates TEMPLATES =
			GWT.create(CellTemplates.class);
	private CellTable<RowData> headerTable;
	private CellTable<RowData> valuesTable;
	private Label emptyLabel;
	private Label emptyInfo;
	private AppW app;
	private ScrollPanel valueScroller;
	private List<RowData> rows = new ArrayList<>();
	private NoDragImage moreImg;
	private FlowPanel tvMainScrollPanel;

	/**
	 * Class to wrap callback after column delete.
	 *
	 * @author laszlo.
	 *
	 */
	private class ColumnDelete implements Runnable {
		Runnable cb = null;
		private int column = -1;
		private Element elem;

		ColumnDelete(int column, Element elem, Runnable cb) {
			this.column = column;
			this.elem = elem;
			this.cb = cb;
		}

		@Override
		public void run() {
			onDeleteColumn(column, elem, cb);
		}
	}

	/**
	 * @author laszlo
	 *
	 */
	public class OuterPanel extends ScrollPanel {

		@Override
		public void onResize() {
			syncHeaderSizes();
		}
	}

	private class RowData {
		private int row;

		public RowData(int row) {
			this.setRow(row);
		}

		/**
		 * @return the column header.
		 */
		public String getHeader() {
			return view.getTableValuesModel().getHeaderAt(1);
		}

		/**
		 *
		 * @param col
		 *            the column
		 * @return the cell value
		 */
		public String getValue(int col) {
			if (getRow() < view.getTableValuesModel().getRowCount()
					&& col < view.getTableValuesModel().getColumnCount()) {
				return view.getTableValuesModel().getCellAt(getRow(), col);
			}
			return "";
		}

		public int getRow() {
			return row;
		}

		public void setRow(int row) {
			this.row = row;
		}
	}

	/**
	 * @param app
	 *            {@link AppW}.
	 */
	public TableValuesPanel(AppW app) {
		super();
		this.app = app;
		view = (TableValuesView) app.getGuiManager().getTableValuesView();
		view.getTableValuesModel().registerListener(this);
		createGUI();
	}

	private AppW getApp() {
		return app;
	}

	/**
	 * Sync header sizes with content column widths
	 */
	protected void syncHeaderSizes() {

		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {

			@Override
			public void execute() {
				doSyncHeaderSizes();
			}
		});
	}

	/**
	 * Sync header sizes to value table.
	 */
	void doSyncHeaderSizes() {
		NodeList<Element> tableRows = valuesTable.getElement().getElementsByTagName("tbody")
				.getItem(0)
				.getElementsByTagName("tr");
		if (tableRows.getLength() == 0) {
			return;
		}

		NodeList<Element> firstRow = tableRows.getItem(0).getElementsByTagName("td");

		for (int i = 0; i < valuesTable.getColumnCount(); i++) {
			int w = firstRow.getItem(i).getOffsetWidth();
			headerTable.setColumnWidth(i, w + "px");
		}

		headerTable.getElement().getStyle().setWidth(valuesTable.getOffsetWidth(), Unit.PX);
	}

	private void createGUI() {
		tvMainScrollPanel = new FlowPanel();

		headerTable = new CellTable<>();
		headerTable = new CellTable<>();
		headerTable.addStyleName("tvHeader");

		valuesTable = new CellTable<>();
		valuesTable.addStyleName("tvValues");

		valueScroller = new ScrollPanel();
		valueScroller.addStyleName("tvValueScroller");

		addHeaderClickHandler();
	}

	/**
	 * Refresh header and data.
	 */
	public void update() {
		if (view.isEmpty()) {
			buildEmptyView();
		} else {
			buildTable();
		}
		setParentStyle();
	}

	private void buildTable() {
		clear();
		removeStyleName("emptyTablePanel");
		addStyleName("tableViewMain");
		TableUtils.clear(valuesTable);
		TableUtils.clear(headerTable);

		addColumnsForTable(headerTable);

		addValuesForTable(valuesTable);
		fillValuesTable();

		valueScroller.setWidget(valuesTable);
		createStickyHeader();
		tvMainScrollPanel.add(valueScroller);
		tvMainScrollPanel.addStyleName("tvMainScrollPanel");
		syncHeaderSizes();

		add(tvMainScrollPanel);
	}

	private void createStickyHeader() {
		final ScrollPanel headerScroller = new ScrollPanel();
		final FlowPanel headerMain = new FlowPanel();
		headerMain.add(headerTable);
		headerScroller.add(headerMain);
		headerScroller.addStyleName("tvHeaderScroller");
		tvMainScrollPanel.clear();
		tvMainScrollPanel.add(headerScroller);

		OuterPanel outerScrollPanel = new OuterPanel(); // used for horizontal
												// scrolling
		outerScrollPanel.addStyleName("outerScrollPanel");
		outerScrollPanel.add(tvMainScrollPanel);

		valueScroller.addScrollHandler(new ScrollHandler() {
			@Override
			public void onScroll(ScrollEvent event) {
				syncScrollPosition(headerScroller, headerMain);
			}
		});
	}

	/**
	 * Sync scroll position of the header and the values table.
	 *
	 * @param headerScroller
	 *            scroll panel for the header.
	 * @param headerMain
	 *            Header main panel that contains the table.
	 */
	void syncScrollPosition(ScrollPanel headerScroller, FlowPanel headerMain) {
		int scrollPosition = valueScroller.getHorizontalScrollPosition();
		if (headerMain.getOffsetWidth() < valueScroller.getOffsetWidth() + scrollPosition) {
			headerMain.setWidth((valueScroller.getOffsetWidth() + scrollPosition) + "px");
		}
		headerScroller.setHorizontalScrollPosition(scrollPosition);
	}

	private void fillValuesTable() {
		rows.clear();
		for (int row = 0; row < view.getTableValuesModel().getRowCount(); row++) {
			rows.add(new RowData(row));
		}

		valuesTable.setRowCount(rows.size());
		valuesTable.setVisibleRange(0, rows.size());
		valuesTable.setRowData(0, rows);
		valuesTable.setVisible(true);
	}

	private void buildEmptyView() {
		clear();
		addStyleName("emptyTablePanel");
		NoDragImage emptyImage = new NoDragImage(
				MaterialDesignResources.INSTANCE.toolbar_table_view_black(),
				56);
		emptyImage.getElement().setAttribute("role", "decoration");
		emptyImage.addStyleName("emptyTableImage");
		FlowPanel emptyImageWrap = new FlowPanel();
		emptyImageWrap.add(emptyImage);
		emptyLabel = new Label();
		emptyLabel.addStyleName("emptyTableLabel");
		emptyInfo = new Label();
		emptyInfo.addStyleName("emptyTableInfo");
		emptyImageWrap.addStyleName("emptyTableImageWrap");
		add(emptyImageWrap);
		add(emptyLabel);
		add(emptyInfo);
		setParentStyle();
		setLabels();
	}

	private void setParentStyle() {
		Element parent = getElement().getParentElement();
		if (parent == null) {
			return;
		}
		if (view.isEmpty()) {
			parent.addClassName("tableViewParent");
		} else {
			parent.removeClassName("tableViewParent");
		}

	}

	@Override
	public void setLabels() {
		if (emptyLabel != null) {
			emptyLabel.setText(app.getLocalization().getMenu("TableValuesEmptyTitle"));
			emptyInfo.setText(app.getLocalization().getMenu("TableValuesEmptyDescription"));
		}
	}

	private static Column<RowData, SafeHtml> getColumnName() {
		Column<RowData, SafeHtml> nameColumn = new Column<RowData, SafeHtml>(
				new SafeHtmlCell()) {

			@Override
			public SafeHtml getValue(RowData object) {
				return SafeHtmlUtils.fromTrustedString(object.getHeader());
			}
		};
		return nameColumn;
	}

	private static Column<RowData, SafeHtml> getColumnValue(final int col,
			final TableValuesDimensions dimensions) {
		Column<RowData, SafeHtml> column = new Column<RowData, SafeHtml>(new SafeHtmlCell()) {

			@Override
			public SafeHtml getValue(RowData object) {
				String valStr = object.getValue(col);
				boolean empty = "".equals(valStr);
				SafeHtml value = SafeHtmlUtils.fromSafeConstant(valStr);
				int width = empty ? 0 : getColumnWidth(dimensions, col);
				int height = empty ? 0 : STRICT_ROW_HEIGHT;
				SafeHtml cell = TEMPLATES.cell(value, width, height);

				return cell;
			}
		};
		return column;
	}

	private NoDragImage getMoreImage() {
		if (moreImg == null) {
			moreImg = new NoDragImage(MaterialDesignResources.INSTANCE.more_vert_black(), 24);
		}
		return moreImg;
	}

	/**
	 * Only call this from constructor
	 */
	private void addHeaderClickHandler() {
		ClickHandler popupMenuClickHandler = new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Element el = Element
						.as(event.getNativeEvent().getEventTarget());
				if (el != null && el.getParentNode() != null && el
						.getParentElement().hasClassName("MyToggleButton")) {
					Node buttonParent = el.getParentNode().getParentNode();
					toggleButtonClick(buttonParent.getParentNode(), el);
				}
			}
		};
		headerTable.addHandler(popupMenuClickHandler, ClickEvent.getType());
	}

	/**
	 * @param buttonParent
	 *            parent of the button
	 * @param el
	 *            element for positioning
	 */
	protected void toggleButtonClick(Node buttonParent, Element el) {
		if (buttonParent != null && buttonParent.getParentNode() != null
				&& buttonParent.getParentNode().getParentElement() != null) {
			// parent tag with the header children
			Element parent = buttonParent.getParentNode().getParentElement();
			// header column which was clicked on
			Node currHeaderCell = buttonParent.getParentNode();
			// get list of header cells
			NodeList<Node> headerNodes = parent.getChildNodes();
			for (int i = 0; i < headerNodes.getLength(); i++) {
				Node node = headerNodes.getItem(i);
				// check if header cell is the one it was clicked on
				if (node.equals(currHeaderCell)) {
					new ContextMenuTV(getApp(), i > 0 ? view.getGeoAt(i - 1) : null,
							i - 1).show(
									new GPoint(el.getAbsoluteLeft(),
											el.getAbsoluteTop() - 8));
				}
			}
		}
	}

	private SafeHtml getHeaderHtml(final int col) {
		FlowPanel p = new FlowPanel();
		p.add(new Label(view.getTableValuesModel().getHeaderAt(col)));
		MyToggleButtonW btn = new MyToggleButtonW(getMoreImage());
		p.add(btn);
		SafeHtml html = SafeHtmlUtils.fromTrustedString(p.getElement().getInnerHTML());
		TableValuesDimensions dimensions = view.getTableValuesDimensions();
		return TEMPLATES.cell(html, getColumnWidth(dimensions, col), TABLE_HEADER_HEIGHT);
	}

	/**
	 * Gives the preferred width of a column.
	 *
	 * @param dimensions
	 *            The column sizes
	 * @param column
	 *            particular column index.
	 * @return the calculated width of the column.
	 */
	static int getColumnWidth(TableValuesDimensions dimensions, int column) {
		int w = Math.max(dimensions.getColumnWidth(column), dimensions.getHeaderWidth(column))
				+ VALUE_RIGHT_MARGIN;
		if (column == 0) {
			w += X_LEFT_PADDING;
		}
		return Math.max(w, MIN_COLUMN_WIDTH + X_LEFT_PADDING);
	}

	private void addColumnsForTable(CellTable<RowData> tb) {
		TableValuesModel m = view.getTableValuesModel();
		for (int i = 0; i < m.getColumnCount(); i++) {
			Column<RowData, ?> col = getColumnName();
			if (col != null) {
				tb.addColumn(col, getHeaderHtml(i));
			}
		}
	}

	private void addValuesForTable(CellTable<RowData> tb) {
		TableValuesModel m = view.getTableValuesModel();
		for (int column = 0; column < m.getColumnCount(); column++) {
			Column<RowData, ?> col = getColumnValue(column, view.getTableValuesDimensions());
			tb.addColumn(col);
		}
	}

	/**
	 * Sets height of the view.
	 *
	 * @param height
	 *            to set.
	 */
	public void setHeight(int height) {
		valueScroller.getElement().getStyle().setHeight(height - TOOLBAR_HEADER_HEIGHT,
				Unit.PX);
	}

	/**
	 * @author Balazs
	 *
	 */
	public interface CellTemplates extends SafeHtmlTemplates {
		/**
		 * @param message
		 *            of the cell.
		 * @param width
		 *            of the cell.
		 * @param height
		 *            of the cell.
		 * @return HTML representation of the cell content.
		 */
		@SafeHtmlTemplates.Template("<div style=\"width:{1}px;height:{2}px;line-height:{2}px;\""
				+ "class=\"cell\"><div class=\"content\">{0}</div></div>")
		SafeHtml cell(SafeHtml message, int width, int height);
	}

	/**
	 * @param column
	 *            to get
	 * @return the list of the specified value column elements (without the header).
	 */
	private static NodeList<Element> getColumnElements(int column) {
		// gives the (column+1)th element of each row of the value table
		return Dom.querySelectorAll(".tvValues tr td:nth-child(" + (column + 1) + ") .cell");
	}

	/**
	 * @param column
	 *            to get
	 * @return the header element.
	 */
	private static Element getHeaderElement(int column) {
		// gives the (column+1)th element of the header row.
		NodeList<Element> list = Dom
				.querySelectorAll(".tvHeader tr th:nth-child(" + (column + 1) + ") .cell");
		return list != null ? list.getItem(0) : null;
	}

	private boolean isXDeleted() {
		return view.getTableValuesModel().getColumnCount() == 0;
	}

	/**
	 * Deletes the specified column from the view.
	 *
	 * @param column
	 *            column to delete.
	 * @param cb
	 *            to run on transition end.
	 */
	public void deleteColumn(int column, Runnable cb) {
		int col = column;

		NodeList<Element> elems = getColumnElements(col);
		Element header = getHeaderElement(col);

		if (elems == null || elems.getLength() == 0 || header == null) {
			return;
		}

		int tableWidth = valuesTable.getOffsetWidth() - header.getOffsetWidth();

		header.addClassName("delete");
		// null check for cb further down
		CSSEvents.runOnTransition(new ColumnDelete(col, header, cb), header,
				"delete");

		for (int i = 0; i < elems.getLength(); i++) {
			Element e = elems.getItem(i);
			e.addClassName("delete");
		}
		headerTable.getElement().getStyle().setWidth(tableWidth, Unit.PX);

	}

	/**
	 * Runs on column delete.
	 *
	 * @param column
	 *            the deleted column number.
	 *
	 * @param header
	 *            The table header HTML element
	 *
	 * @param cb
	 *            custom callback to run on column delete.
	 */
	void onDeleteColumn(int column, Element header, Runnable cb) {
		if (isXDeleted()) {
			if (cb != null) {
				cb.run();
			}
		} else {
			header.getParentElement().removeFromParent();
		}
		if (view.isEmpty()) {
			update();
		}
	}

	@Override
	public void notifyColumnRemoved(TableValuesModel model, GeoEvaluatable evaluatable,
			int column) {
		deleteColumn(column, null);

	}

	@Override
	public void notifyColumnChanged(TableValuesModel model, GeoEvaluatable evaluatable,
			int column) {
		update();
	}

	@Override
	public void notifyColumnAdded(TableValuesModel model, GeoEvaluatable evaluatable, int column) {
		update();
	}

	@Override
	public void notifyColumnHeaderChanged(TableValuesModel model, GeoEvaluatable evaluatable,
			int column) {
		update();
	}

	@Override
	public void notifyDatasetChanged(TableValuesModel model) {
		update();
	}

	@Override
	public void onAttach() {
		super.onAttach();
		this.setParentStyle();
	}
}
