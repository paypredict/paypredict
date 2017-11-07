package io.github.paypredict.web.ui

import com.vaadin.ui.Component

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 11/7/2017.
 */
interface DashboardComponent {
    val name: String
    val title: String
    val component: Component
}