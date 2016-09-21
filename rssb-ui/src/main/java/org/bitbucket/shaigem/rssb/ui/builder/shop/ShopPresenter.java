package org.bitbucket.shaigem.rssb.ui.builder.shop;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import org.bitbucket.shaigem.rssb.event.ShopSaveEvent;
import org.bitbucket.shaigem.rssb.fx.ShopTab;
import org.bitbucket.shaigem.rssb.fx.control.RuneScapeButton;
import org.bitbucket.shaigem.rssb.fx.control.ShopDisplayRadioButton;
import org.bitbucket.shaigem.rssb.fx.control.dialog.MaterialDesignInputDialog;
import org.bitbucket.shaigem.rssb.model.DragItemManager;
import org.bitbucket.shaigem.rssb.model.ShopItemClipboardManager;
import org.bitbucket.shaigem.rssb.model.ShopItemSelectionModel;
import org.bitbucket.shaigem.rssb.model.ShopRepository;
import org.bitbucket.shaigem.rssb.model.item.Item;
import org.bitbucket.shaigem.rssb.model.shop.Shop;
import org.bitbucket.shaigem.rssb.ui.builder.BuilderWindowPresenter;
import org.bitbucket.shaigem.rssb.ui.builder.shop.item.ShopItemView;
import org.bitbucket.shaigem.rssb.util.AlertDialogUtil;
import org.bitbucket.shaigem.rssb.util.ItemAmountUtil;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanPropertyUtils;
import org.sejda.eventstudio.DefaultEventStudio;

import javax.inject.Inject;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created on 2015-08-11.
 */
public class ShopPresenter implements Initializable {


    private Shop shop;

    private TextField nameTextField;

    private ShopItemSelectionModel selectionModel = new ShopItemSelectionModel();

    private ShopTab tab;

    private BooleanProperty modified;

    private BuilderWindowPresenter mainWindowPresenter;

    private ObservableList<PropertySheet.Item> shopBeanProperties;

    @Inject
    DragItemManager dragItemManager;

    @Inject
    ShopRepository shopRepository;

    @FXML
    AnchorPane rootPane;

    @FXML
    Pane group;

    @FXML
    ScrollPane scrollPane;

    @FXML
    ImageView shopImageView;

    /**
     * Used as a visual to show if a shop is a general store.
     * Hidden by default.
     */
    @FXML
    ImageView generalStoreImageView;

    @FXML
    StackPane stackPane;
    @FXML
    TilePane shopItemPane;

    @FXML
    Label shopNameLabel;

    @FXML
    HBox itemDisplayStyleHBox;

    @FXML
    TilePane actionsPane;
    @FXML
    BorderPane bottomMiddlePane;

    @FXML
    ImageView selectedItemImageView;
    @FXML
    Label selectedItemNameLabel;
    @FXML
    Label selectedItemIdLabel;
    @FXML
    TilePane selectedItemInfoTilePane;
    @FXML
    MenuItem changeAmountMenuItem;
    @FXML
    MenuItem deleteMenuItem;
    @FXML
    MenuItem copyMenuItem;
    @FXML
    MenuItem pasteMenuItem;

    @Inject
    DefaultEventStudio eventStudio;

    private ObjectProperty<ShopDisplayRadioButton.DisplayMode> displayModeProperty;

    private BooleanProperty generalStoreImageVisibility;

    public void initialize(URL location, ResourceBundle resources) {
        displayModeProperty = new SimpleObjectProperty<>();
        modified = new SimpleBooleanProperty();
        generalStoreImageVisibility = new SimpleBooleanProperty();
        generalStoreImageView.visibleProperty().bind(generalStoreImageVisibility);
        bindDisablePropertyForMenuItems();
        setupActionsPane();
        setupSelectedItemInformationArea();
        setupResourceImages();
        setupNameTextField();
        setupItemDisplayButtons();
        registerDropEvents();
        listenForModifiedPropertyChange();
        //scrollWhenHeightIncreases();
        handleNameChangeEvents();
    }


    public static ListChangeListener<Shop> onPropertyChangeListener(ShopPresenter presenter) {
        return c -> {
            while (c.next()) {
                if (c.wasUpdated()) {
                    presenter.markAsModified();
                }
            }

        };
    }

    public final void cleanup() {
        shopBeanProperties = null;
        shop = null;
        selectionModel = null;
    }

    public void selectAllItems() {
        if (shopItemPane.getChildren().isEmpty()) {
            return;
        }
        shopItemPane.getChildren().forEach((node -> {
            ShopItemView itemView = (ShopItemView) node;
            selectionModel.addToSelection(itemView, false);
        }));
    }

    public void setShop(Shop shop) {
        this.shop = shop;
        shopDidChange();
        listenForShopItemsChanges();
    }

    public void setTab(ShopTab tab) {
        this.tab = tab;
    }

    public ShopTab getTab() {
        return tab;
    }

    public void swapItems(int fromIndex, int toIndex) {
        ObservableList<Node> workingList = FXCollections.observableArrayList(shopItemPane.getChildren());
        Collections.swap(workingList, fromIndex, toIndex);
        shopItemPane.getChildren().setAll(workingList);
    }

    /**
     * Add a item to be displayed in the shop.
     *
     * @param item           the {@link Item} to add
     * @param resetSelection if selection should be reset
     */
    public void addItem(Item item, boolean resetSelection) {
        ShopItemView shopItemView = new ShopItemView();
        shopItemView.getPresenter().setShopPresenter(this);
        shopItemView.getPresenter().setItem(item.copy()); // create a copy
        if (resetSelection) { // sets only the newly added item as selected
            selectionModel.setSelected(shopItemView);
        } else {
            selectionModel.addToSelection(shopItemView, true);
        }
        shopItemPane.getChildren().add(shopItemView);
    }


    /**
     * Adds all items to the shop view from the provided collection.
     *
     * @param itemCollection the collection to add items from
     */
    public void addItems(Collection<Item> itemCollection) {
        final boolean multipleItems = itemCollection.size() > 1;
        if (multipleItems) {
            if (getSelectionModel().hasAnySelection()) {
                // clears any selection if we are adding multiple items
                // Reason is so we can select just the items that are being added
                getSelectionModel().clearSelection();
            }
        }
        for (Item item : itemCollection) {
            addItem(item, !(multipleItems));
        }
        if (!multipleItems) {
            // select the last item
            if (!shopItemPane.getChildren().isEmpty())
                getSelectionModel().setSelected((ShopItemView)
                        shopItemPane.getChildren().get(shopItemPane.getChildren().size() - 1));
        }
    }

    /**
     * Deletes all selected shop items.
     */
    public void deleteSelectedItems() {
        final ObservableSet<ShopItemView> selectedItems =
                selectionModel.getSelectedShopItems();

        if (selectedItems.isEmpty()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.YES);
        alert.setTitle("Delete Item");
        alert.setContentText(String.format("Are you sure you want to delete %s?",
                selectedItems.size() > 1 ? "these items" : "this item"));
        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType.equals(ButtonType.YES)) {
                ShopItemView lastDeletedItem = null;
                if (!selectedItems.isEmpty())
                    lastDeletedItem = (ShopItemView) selectedItems.toArray()[selectedItems.size() - 1];
                int lastDeletedIndex = shopItemPane.getChildren().indexOf(lastDeletedItem);

                shopItemPane.getChildren().removeAll(selectedItems);

                int indexToSelect = lastDeletedIndex;
                if (lastDeletedIndex > shopItemPane.getChildren().size() - 1) {
                    indexToSelect = shopItemPane.getChildren().size() - 1;
                }

                if (indexToSelect != -1) {
                    selectionModel.setSelected((ShopItemView) shopItemPane.getChildren().get(indexToSelect));
                }
            }
        });
    }


    public void save() {
        // mainWindowPresenter.getExplorerPresenter().checkForDuplicateKeys(shop);
        boolean needsSaving = hasBeenModified();
        if (needsSaving) {
            shop.getItems().clear();
            shopItemPane.getChildren().forEach(node -> {
                ShopItemView view = (ShopItemView) node;
                shop.getItems().add(view.getPresenter().getItem());
            });
            shop.setName(shopNameLabel.getText());
            markAsNotModified();
        }
        eventStudio.broadcast(new ShopSaveEvent(this, needsSaving));
    }

    public void updateSelectionInformation() {
        if (selectionModel.hasMultipleSelected()) {
            selectedItemNameLabel.setText("Multiple Items");
            selectedItemIdLabel.setVisible(false);
            selectedItemImageView.setImage(null);
        } else {
            if (selectionModel.hasAnySelection()) {
                selectionModel.getSelectedShopItems().forEach((itemView -> {
                    Item item = itemView.getPresenter().getItem();
                    selectedItemNameLabel.setText(item.getName());
                    selectedItemIdLabel.setVisible(true);
                    selectedItemIdLabel.setText("ID: " + item.getId());
                    selectedItemImageView.setImage(item.getImage());
                }));
            } else {
                selectedItemNameLabel.setText("No Selection");
                selectedItemIdLabel.setVisible(false);
                selectedItemImageView.setImage(null);
            }
        }
    }

    /**
     * Clears all of the items from the shop view.
     */
    public void deleteAllItems(boolean confirmation) {
        if (confirmation) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.YES);
            alert.setTitle("Delete All Items");
            alert.setContentText(("Are you sure you want to delete all items?"));
            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType.equals(ButtonType.YES)) {
                    shopItemPane.getChildren().clear();
                }
            });
            return;
        }
        shopItemPane.getChildren().clear();
    }

    /**
     * Changes the visibility of the general store images view.
     *
     * @param visible if true then the general store marker images will show.
     *                Otherwise, it will be hidden from the shop.
     */

    public void setGeneralStoreVisibility(boolean visible) {
        generalStoreImageView.setVisible(visible);

    }

    public void setShopNameLabel(String name) {
        shopNameLabel.setText(name);
    }

    private void shopDidChange() {
        deleteAllItems(false);
        setShopNameLabel(shop.getName());
        generalStoreImageVisibility.bind(shop.generalStoreProperty());
        addItems(shop.getItems());
        if (!shopItemPane.getChildren().isEmpty()) {
            ShopItemView firstShopItem = (ShopItemView) shopItemPane.getChildren().get(0);
            selectionModel.setSelected(firstShopItem);
        }
        shopNameLabel.textProperty().addListener(((observable, oldValue, newValue) -> setModified(true)));
        // ShopItemView lastItem = (ShopItemView) shopItemPane.getChildren().get
        //        (shopItemPane.getChildren().size() - 1);
        //  selectionModel.setSelected(lastItem);
    }

    @FXML
    public void onCopyAction() {
        final boolean canCopy = selectionModel.hasAnySelection();
        if (canCopy) {
            ShopItemClipboardManager.getInstance().setItems
                    (selectionModel.getSelectedShopItems().stream().map(shopItemView ->
                            shopItemView.getPresenter().getItem()).collect(Collectors.toList()));
        }
    }

    @FXML
    public void onPasteAction() {
        final boolean canPaste = ShopItemClipboardManager.getInstance().hasItems();
        if (canPaste) {
            final ObservableSet<Item> itemContent = ShopItemClipboardManager.getInstance().getItems();
            addItems(itemContent);
        }
    }

    @FXML
    void onDeleteAction() {
        deleteSelectedItems();
    }

    @FXML
    public void onChangeAmountAction() {
        if (!selectionModel.hasAnySelection()) {
            return;
        }
        MaterialDesignInputDialog inputDialog = new MaterialDesignInputDialog();
        inputDialog.setTitle("Change Amount");
        inputDialog.setHeaderText("Change amount for multiple items");
        inputDialog.getContentPane().getInputTextField().setPromptText
                ("Enter amount (10, 10k, 100k, 1m, etc.)");
        Optional<String> result = inputDialog.showAndWaitWithInput();
        result.ifPresent(value -> {
            if (value.isEmpty()) {
                return;
            }
            int realAmount = ItemAmountUtil.getUnformattedAmount(value);
            selectionModel.getSelectedShopItems().forEach(shopItemView ->
                    shopItemView.getPresenter().getItem().setAmount(realAmount));
        });
    }

    private void bindDisablePropertyForMenuItems() {
        BooleanBinding selectionListIsEmpty = Bindings.isEmpty(selectionModel.getSelectedShopItems());
        // disable menu items when there are no selected items
        changeAmountMenuItem.disableProperty().bind(selectionListIsEmpty);
        deleteMenuItem.disableProperty().bind(selectionListIsEmpty);
        copyMenuItem.disableProperty().bind(selectionListIsEmpty);
        // disable menu items when there are no copied items in the item clipboard
        pasteMenuItem.disableProperty().bind(Bindings.isEmpty(ShopItemClipboardManager.getInstance().getItems()));
    }

    private void scrollWhenHeightIncreases() {
        shopItemPane.heightProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > oldValue.doubleValue() && oldValue.doubleValue() != 0.0) { //if height got bigger
                scrollPane.setVvalue(scrollPane.getVmax());
            }
        }));
    }

    private void setupSelectedItemInformationArea() {
        selectionModel.getSelectedShopItems().addListener((SetChangeListener<? super ShopItemView>) change
                -> updateSelectionInformation());
        RuneScapeButton editIndexButton = new RuneScapeButton("Change Item...");
        editIndexButton.setTooltip(new Tooltip("Change the selected item to another item"));
        editIndexButton.setOnAction(event -> {
            if (selectionModel.hasAnySelection() && !selectionModel.hasMultipleSelected()) {
                selectionModel.getSelectedShopItems().forEach((shopItemView ->
                        shopItemView.getPresenter().openEditIndexDialog()));
            }
        });
        selectedItemInfoTilePane.getChildren().add(editIndexButton);
        selectedItemInfoTilePane.visibleProperty().bind(Bindings.isNotNull(selectedItemImageView.imageProperty()));
    }


    private void setupActionsPane() {
      /*  RuneScapeButton addButton = new RuneScapeButton("Add By Id");
        addButton.setOnAction((e) -> openAddItemByIndexDialog());

        RuneScapeButton deleteButton = new RuneScapeButton("Delete Item");
        deleteButton.disableProperty().bind(Bindings.isEmpty(selectionModel.getSelectedShopItems()));
        deleteButton.setOnAction((event -> deleteItem(selectionModel.getSelectedShopItems())));
        RuneScapeButton deleteAllButton = new RuneScapeButton("Delete All");
        deleteAllButton.setOnAction((event1 -> clear()));

        RuneScapeButton saveButton = new RuneScapeButton("Save");
        saveButton.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 12));
        saveButton.setOnAction((event -> save()));
        actionsPane.getChildren().addAll(addButton, deleteAllButton);
        bottomMiddlePane.setCenter(saveButton);
        */
    }


    public void openAddItemByIndexDialog() {
        MaterialDesignInputDialog inputDialog = new MaterialDesignInputDialog("0");
        inputDialog.setTitle("Input Dialog");
        inputDialog.setHeaderText("Add by Item ID");
        inputDialog.setPromptText("Enter Id");
        inputDialog.showAndWaitWithInput().ifPresent(value -> {
            try {
                int id = Integer.parseInt(value);
                addItem(new Item(id), true);
            } catch (NumberFormatException exc) {
                ButtonType tryAgainButton = new ButtonType("Try Again", ButtonBar.ButtonData.OK_DONE);
                ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
                Alert alert
                        = AlertDialogUtil.createExceptionDialog(exc);
                alert.setTitle("Error With Input");
                alert.getButtonTypes().setAll(tryAgainButton, closeButton);
                alert.setHeaderText("Invalid Input");
                alert.setContentText("Cannot parse input. The input must be a number!");
                Optional<ButtonType> response = alert.showAndWait();
                response.ifPresent(buttonType -> {
                    if (buttonType == tryAgainButton) {
                        openAddItemByIndexDialog();
                    }
                });
            }
        });
    }

    private void setupItemDisplayButtons() {
        ToggleGroup group = new ToggleGroup();
        ShopDisplayRadioButton expandedButton = new
                ShopDisplayRadioButton(ShopDisplayRadioButton.DisplayMode.EXPANDED);
        ShopDisplayRadioButton iconButton = new ShopDisplayRadioButton(ShopDisplayRadioButton.
                DisplayMode.ICON);

        /* Listen to the button toggle and sets the toggled display mode */
        group.selectedToggleProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                ShopDisplayRadioButton button = (ShopDisplayRadioButton) newValue.getToggleGroup().getSelectedToggle();
                displayModeProperty.set(button.getDisplayMode());
            }
        }));

        /* Listens for any changes to the display mode and sets the correct toggle button */
        displayModeProperty.addListener(((observable, oldValue, newValue) -> {
            //in case the display mode gets set manually from code
            if (newValue == ShopDisplayRadioButton.DisplayMode.EXPANDED)
                group.selectToggle(expandedButton);
            else
                group.selectToggle(iconButton);

        }));
        expandedButton.setToggleGroup(group);
        iconButton.setToggleGroup(group);
        expandedButton.setTooltip(new Tooltip("Toggle expanded item view"));
        iconButton.setTooltip(new Tooltip("Toggle icon item view"));
        itemDisplayStyleHBox.getChildren().addAll(expandedButton, iconButton);
    }

    private void setupNameTextField() {
        nameTextField = new TextField();
        nameTextField.getStyleClass().add("name-text-field");
        nameTextField.setPrefWidth(shopNameLabel.getPrefWidth());
        nameTextField.setPrefHeight(shopNameLabel.getPrefHeight());
        nameTextField.setAlignment(Pos.CENTER);
        nameTextField.setFont(Font.font(15));
    }

    private void setupResourceImages() {
        shopImageView.setImage(new Image(getClass().getClassLoader().getResourceAsStream("images/shop.png")));
        generalStoreImageView.setImage
                (new Image(getClass().getClassLoader().getResourceAsStream("images/gen_store.png")));
    }

    private void requestNameChange() {
        nameTextField.setText(shopNameLabel.getText());
        shopNameLabel.setGraphic(nameTextField);

        Platform.runLater(() -> {//fixes issue that occurs when tab support was added
            nameTextField.selectAll();
            nameTextField.requestFocus();
        });
    }

    private void listenForShopItemsChanges() {
        shopItemPane.getChildren().addListener((ListChangeListener<? super Node>) change -> {
            while (change.next()) {
                if (!hasBeenModified())
                    markAsModified();
                if (change.wasRemoved() && !change.wasReplaced()) {
                    List<? extends Node> removed = change.getRemoved();
                    removed.forEach((e) -> {
                        ShopItemView item = (ShopItemView) e;
                        selectionModel.deselect(item);
                        item.getPresenter().onRemoval();
                    });
                }
            }
        });
    }

    private void registerDropEvents() {
        shopItemPane.setOnDragOver((event) -> {
            List<Item> source = dragItemManager.getItems();
            if (event.getDragboard().hasString() && !source.isEmpty()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        shopItemPane.setOnDragDropped((event) -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                List<Item> dragList = dragItemManager.getItems();
                final boolean multipleItems = dragList.size() > 1;
                if (multipleItems) {
                    if (getSelectionModel().hasAnySelection()) {
                        // clears any selection if we are adding multiple items
                        // Reason is so we can select just the items that are being dragged
                        getSelectionModel().clearSelection();
                    }
                }
                // if we are adding multiple items, select all of those items ONLY
                dragList.forEach((item -> addItem(item, !multipleItems)));
                success = true;

            }
            dragItemManager.onDropComplete();
            event.setDropCompleted(success);
            event.consume();
        });
    }


    private void handleNameChangeEvents() {
        shopNameLabel.setOnMousePressed((event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                if (event.getClickCount() == 2) {
                    requestNameChange();
                }
            }
        }
        ));

        // If the user presses enter while editing the shop label,
        // it will set the shop name.
        nameTextField.setOnAction((event) -> {
            shopNameLabel.setGraphic(null);
            setShopNameLabel(nameTextField.getText());
        });


        nameTextField.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!newValue) { // not focused
                shopNameLabel.setGraphic(null);
            }
        }));
    }

    private void listenForModifiedPropertyChange() {
        modified.addListener(((observable, oldValue, newValue) ->
                tab.setText(newValue ? shop.toString() + "(*)" : shop.toString())));
    }

    public void markAsModified() {
        setModified(true);
    }

    public void markAsNotModified() {
        setModified(false);
    }

    private void setModified(boolean modified) {
        this.modified.setValue(modified);
    }

    public boolean hasBeenModified() {
        return modified.get();
    }

    public BooleanProperty modifiedProperty() {
        return modified;
    }

    public void setDisplayMode(ShopDisplayRadioButton.DisplayMode mode) {
        displayModeProperty.set(mode);
    }

    public ShopDisplayRadioButton.DisplayMode getCurrentDisplayMode() {
        return displayModeProperty.get();
    }

    public ObjectProperty<ShopDisplayRadioButton.DisplayMode> displayModeProperty() {
        return displayModeProperty;
    }

    public Shop getShop() {
        return shop;
    }

    public ShopItemSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public TilePane getShopItemPane() {
        return shopItemPane;
    }

    public DragItemManager getDragItemManager() {
        return dragItemManager;
    }

    public BuilderWindowPresenter getMainWindowPresenter() {
        return mainWindowPresenter;
    }

    public ObservableList<PropertySheet.Item> getPropertySheetItemsForShop() {
        if (shopBeanProperties == null) {
            shopBeanProperties = BeanPropertyUtils.getProperties(shop, propertyDescriptor -> !propertyDescriptor.getName().equals("name")
                    && !propertyDescriptor.getName().equals("items")
                    && !propertyDescriptor.getName().equals("customPropertiesToObserve"));
        }
        return shopBeanProperties;
    }

    public void setMainWindowPresenter(BuilderWindowPresenter mainWindowPresenter) {
        this.mainWindowPresenter = mainWindowPresenter;
    }
}
