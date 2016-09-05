package org.bitbucket.shaigem.rssb.ui.dashboard.format;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import org.bitbucket.shaigem.rssb.event.RefreshDashboardEvent;
import org.bitbucket.shaigem.rssb.event.SetActiveFormatPluginRequest;
import org.bitbucket.shaigem.rssb.plugin.BaseShopFormatPlugin;
import org.bitbucket.shaigem.rssb.plugin.RSSBPluginManager;
import org.bitbucket.shaigem.rssb.ui.dashboard.DashboardPresenter;
import org.sejda.eventstudio.DefaultEventStudio;
import org.sejda.eventstudio.annotation.EventListener;

import javax.inject.Inject;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Handles the presentation logic for the pane that is responsible for displaying any shop formats.
 */
public class FormatPluginsPanePresenter implements Initializable {

    private DashboardPresenter dashboardPresenter;

    @FXML
    FlowPane formatItemPane;
    @FXML
    ScrollPane scrollPane;
    @FXML
    Label noPluginsFoundLabel;

    @Inject
    DefaultEventStudio eventStudio;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateDashboard();
        noPluginsFoundLabel.visibleProperty().bind(Bindings.isEmpty(formatItemPane.getChildren()));
        eventStudio.addAnnotatedListeners(this);
    }

    @EventListener
    private void onRefreshDashboard(RefreshDashboardEvent event) {
        FormatDashboardTile tile = new FormatDashboardTile(eventStudio, (BaseShopFormatPlugin) event.getAddedPlugin());
        formatItemPane.getChildren().add(tile);
    }

    @EventListener
    private void onSetActiveFormatRequest(SetActiveFormatPluginRequest request) {
        FormatDashboardTile tile = request.getSource();
        setActiveTile(tile);
        dashboardPresenter.showBuilder(request.getShopFormatPlugin());
    }


    private void setActiveTile(FormatDashboardTile tileToSet) {
        // first set current active tile to inactive
        Optional<FormatDashboardTile> currentActiveTile =
                formatItemPane.getChildren().filtered(node ->
                        node instanceof FormatDashboardTile).stream().map(
                        node -> (FormatDashboardTile) node).filter(FormatDashboardTile::isActive).findFirst();
        currentActiveTile.ifPresent(activeTile -> activeTile.setActive(false));
        // then set the new tile to become active
        tileToSet.setActive(true);
    }


    private void populateDashboard() {
        RSSBPluginManager.INSTANCE.getLoadedPlugins().forEach((shopFormatPlugin -> formatItemPane.getChildren().add(
                new FormatDashboardTile(eventStudio, (BaseShopFormatPlugin) shopFormatPlugin))));
    }

    public void setDashboardPresenter(DashboardPresenter dashboardPresenter) {
        this.dashboardPresenter = dashboardPresenter;
    }
}
