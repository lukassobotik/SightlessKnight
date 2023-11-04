package lukas.sobotik.sightlessknight.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("SightlessKnight")
@Route(value = "", layout = MainLayout.class)
public class HomeView extends VerticalLayout {
    HomeView() {
        H1 practiceMoves = new H1("Practice Moves");
        Button knightButton = new Button("Knight");
        knightButton.addClickListener(e -> {
            knightButton.getUI().ifPresent(ui ->
                    ui.navigate("play/knight"));
        });

        add(practiceMoves, knightButton);


        HorizontalLayout containerLayout = new HorizontalLayout();
        containerLayout.setSizeFull();
        containerLayout.addClassName("column-container");

        Div leftColumn = new Div();
        Div middleColumn = new Div();
        Div rightColumn = new Div();
        leftColumn.addClassName("column");
        leftColumn.addClassName("left-column");
        middleColumn.addClassName("column");
        middleColumn.addClassName("middle-column");
        rightColumn.addClassName("column");
        rightColumn.addClassName("right-column");

        middleColumn.getStyle().set("resize", "horizontal");
        middleColumn.getStyle().set("overflow", "auto");

        String resizeObserverScript = "if (window.ResizeObserver) { " +
                "var resizeTimeout; " +
                "var observer = new ResizeObserver(function(entries) { " +
                "  clearTimeout(resizeTimeout); " +
                "  resizeTimeout = setTimeout(function() { " +
                "    entries.forEach(function(entry) { " +
                "      var width = entry.contentRect.width; " +
                "      entry.target.style.height = width + 'px'; " +
                "    }); " +
                "  }, 250); " + // Adjust the delay (in milliseconds) as needed
                "}); " +
                "observer.observe(this); " +
                "}";
        middleColumn.getElement().executeJs(resizeObserverScript);

        containerLayout.add(leftColumn, middleColumn, rightColumn);
        add(containerLayout);
    }
}
