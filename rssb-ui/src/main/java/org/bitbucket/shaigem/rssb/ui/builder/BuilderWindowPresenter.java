package org.bitbucket.shaigem.rssb.ui.builder;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.bitbucket.shaigem.rssb.event.ActiveFormatPluginChangedEvent;
import org.bitbucket.shaigem.rssb.event.LoadShopsEvent;
import org.bitbucket.shaigem.rssb.event.RemoveShopRequest;
import org.bitbucket.shaigem.rssb.model.ActiveFormatManager;
import org.bitbucket.shaigem.rssb.model.ShopRepository;
import org.bitbucket.shaigem.rssb.model.ShopTabManager;
import org.bitbucket.shaigem.rssb.model.shop.Shop;
import org.bitbucket.shaigem.rssb.plugin.BaseShopFormatPlugin;
import org.bitbucket.shaigem.rssb.plugin.ShopFormat;
import org.bitbucket.shaigem.rssb.ui.builder.explorer.ShopExplorerView;
import org.bitbucket.shaigem.rssb.ui.builder.itemlist.ItemListPresenter;
import org.bitbucket.shaigem.rssb.ui.builder.itemlist.ItemListView;
import org.bitbucket.shaigem.rssb.ui.builder.properties.PropertiesView;
import org.bitbucket.shaigem.rssb.ui.search.SearchPresenter;
import org.bitbucket.shaigem.rssb.ui.search.SearchView;
import org.bitbucket.shaigem.rssb.ui.builder.shop.ShopPresenter;
import org.bitbucket.shaigem.rssb.util.AlertDialogUtil;
import org.sejda.eventstudio.DefaultEventStudio;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * @author Abyss
 */
public class BuilderWindowPresenter implements Initializable {

    private ItemListPresenter itemListPresenter;
    private BooleanProperty defaultExpandedItemDisplay;

    @Inject
    ShopTabManager tabManager;

    @Inject
    ShopRepository repository;

    @Inject
    DefaultEventStudio eventStudio;

    @Inject
    ActiveFormatManager activeFormatManager;

    @FXML
    VBox rootPane;

    @FXML
    ToolBar itemToolBar;
    @FXML
    StackPane itemPane;
    @FXML
    StackPane itemSearchPane;

    @FXML
    StackPane explorerPane;
    @FXML
    Button createNewShopButton;
    @FXML
    Button deleteSelectedShopButton;

    @FXML
    BorderPane shopPane;

    @FXML
    TabPane shopTabPane;

    @FXML
    StackPane propertiesPane;

    @FXML
    MenuItem selectAllMenuItem;
    @FXML
    MenuItem switchFormatMenuItem;
    @FXML
    RadioMenuItem expandedItemDisplayRadioItem;

    public void initialize(URL location, ResourceBundle resources) {
        tabManager.setBuilderWindowPresenter(this);
        defaultExpandedItemDisplay = new SimpleBooleanProperty();
        defaultExpandedItemDisplay.bind(expandedItemDisplayRadioItem.selectedProperty());
        setShopArea();
        setShopExplorerArea();
        setItemListArea();
        setPropertiesArea();
        setSearchField();
        listenForTabSelection();
        selectAllMenuItem.disableProperty().bind(Bindings.isNull(tabManager.currentShopProperty()));
        eventStudio.addAnnotatedListeners(this);
    }

    public void onShow(BaseShopFormatPlugin requestedFormatPlugin) {
        boolean needsChanging = activeFormatManager.getFormatPlugin() != requestedFormatPlugin;
        if (needsChanging) {
            activeFormatManager.setFormatPlugin(requestedFormatPlugin);
            updateStageTitle();
            eventStudio.broadcast(new ActiveFormatPluginChangedEvent(
                    requestedFormatPlugin));
        }
        removeBlurFromWindow();
    }


    @FXML
    public void onNewShopAction() {
        final Shop newShop = activeFormatManager.getFormat().getDefaultShop().copy();
        repository.getMasterShopDefinitions().add(newShop);
    }


    @FXML
    public void onRemoveSelectedShopAction() {
        eventStudio.broadcast(new RemoveShopRequest());
    }


    @FXML
    public void onSwitchFormatAction() {
        Stage builderStage = getStage();
        Stage owner = (Stage) builderStage.getOwner();
        blurWindow();
        owner.show();
        if (owner.getOnCloseRequest() == null) {
            owner.setOnCloseRequest(event -> removeBlurFromWindow());
        }
    }

    @FXML
    public void onOpenAction() {
        final ShopFormat shopFormat = activeFormatManager.getFormat();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Shops");
        //TODO save last directory and use it here

        Optional<File> chosenFile = Optional.ofNullable(chooser.showOpenDialog(getWindow()));

        chosenFile.ifPresent((file -> {
            try {
                repository.populate(shopFormat.load(file));
                eventStudio.broadcast(new LoadShopsEvent());
            } catch (Throwable throwable) {
                Alert exceptionAlert = AlertDialogUtil.createExceptionDialog(throwable);
                exceptionAlert.setHeaderText("Error");
                exceptionAlert.setContentText("An exception was caught while trying to load the shops!");
                exceptionAlert.showAndWait();
            }

        }));
    }

    @FXML
    public void onSelectAllMenuAction() {
        tabManager.getCurrentViewingShop().selectAllItems();
    }


    private void setShopExplorerArea() {
        //TODO separate the buttons (createNewShopButton and deleteSelectedShopButton) to the shopexplorerfxml
        ShopExplorerView shopExplorerView = new ShopExplorerView();
        explorerPane.getChildren().add(shopExplorerView.getViewWithoutRootContainer());

    }

    private void setItemListArea() {
        ItemListView itemListView = new ItemListView();
        itemListPresenter = (ItemListPresenter) itemListView.getPresenter();
        itemPane.getChildren().add(itemListView.getViewWithoutRootContainer());
    }

    private void setPropertiesArea() {
        PropertiesView propertiesView = new PropertiesView();
        propertiesPane.getChildren().add(propertiesView.getViewWithoutRootContainer());
    }

    private void setShopArea() {
        // shopView = new ShopView();
        //shopPresenter = (ShopPresenter) shopView.getPresenter();
        //   shopPane.setCenter(shopPresenter.getNoShopLabel()); TODO show a label when there is no shops open
    }

    private void setSearchField() {
        SearchView itemSearchView = new SearchView();
        SearchPresenter shopListSearchPresenter = (SearchPresenter) itemSearchView.getPresenter();
        shopListSearchPresenter.textProperty().addListener(
                ((observable, oldValue, newValue) -> itemListPresenter.setSearchPattern(newValue)));
        HBox.setHgrow(itemSearchPane, Priority.ALWAYS);
        itemSearchPane.getChildren().add(itemSearchView.getView());

    }


    private void listenForTabSelection() {
        shopTabPane.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) ->
        {
            Optional<ShopPresenter> presenter = tabManager.getPresenterForTab(newValue);
            tabManager.setCurrentShop(presenter.isPresent() ? presenter.get() : null);
        }));
    }

    private void updateStageTitle() {
        getStage().setTitle("Shop Builder [" +
                activeFormatManager.getFormat().descriptor().getName() + "]");
    }

    private final BoxBlur boxBlur = new BoxBlur(5, 5, 2);

    private void blurWindow() {
        rootPane.setEffect(boxBlur);
    }

    private void removeBlurFromWindow() {
        rootPane.setEffect(null);
    }

    public TabPane getShopTabPane() {
        return shopTabPane;
    }

    public boolean byDefaultExpandItemDisplay() {
        return defaultExpandedItemDisplay.get();
    }

    private Window getWindow() {
        return rootPane.getScene().getWindow();
    }

    private Stage getStage() {
        return (Stage) rootPane.getScene().getWindow();
    }
}
