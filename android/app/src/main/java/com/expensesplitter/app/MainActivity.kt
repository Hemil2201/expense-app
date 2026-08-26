package com.expensesplitter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.expensesplitter.app.ui.navigation.ExpenseSplitterNavGraph
import com.expensesplitter.app.ui.theme.ExpenseSplitterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ExpenseSplitterApp).container

        setContent {
            ExpenseSplitterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ExpenseSplitterNavGraph(container = container)
                }
            }
        }
    }
}
