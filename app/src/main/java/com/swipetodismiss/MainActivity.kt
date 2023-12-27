package com.swipetodismiss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DismissDirection
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DismissState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.material3.DismissValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipetodismiss.ui.theme.SwipeToDismissTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwipeToDismissTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FancyList()
                }
            }
        }
    }
}

class FancyViewModel : ViewModel() {
    val persons = mutableStateListOf<Person>(
            Person(1, "Bill", 99),
            Person(2, "Teddy", 88),
            Person(3, "Shelly", 44),
            Person(4, "Alexander", 22),
            Person(5, "Arnold", 5),
            Person(6, "Marcy", 333),
        )

    fun add(person: Person){
        viewModelScope.launch {
            delay(500L)
            persons.add(person)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FancyList() {
    val fancyViewModel : FancyViewModel = viewModel()
    fun deletePerson(
        person: Person,
        index: Int,
        dismissState: DismissState,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState
    ) {
        scope.launch {
            dismissState.reset()
            fancyViewModel.persons.remove(person)
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Deleted " + person.name,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.Dismissed -> {}
                SnackbarResult.ActionPerformed -> {
                   fancyViewModel. persons.add(index, person)
                }
            }
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val personToArchieve: MutableState<Person?> = remember {
        mutableStateOf(null)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        content = { paddingValuesTop ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValuesTop),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(
                    items = fancyViewModel.persons,
                    key = { _, person -> person.id }) { index, person ->

                    val dismissState = rememberDismissState()

                    if (dismissState.isDismissed(direction = DismissDirection.EndToStart)) {
                        deletePerson(
                            person = person,
                            index = index,
                            dismissState = dismissState,
                            scope = scope,
                            snackbarHostState = snackbarHostState
                        )
                    } else if (dismissState.isDismissed(direction = DismissDirection.StartToEnd)) {
                        fancyViewModel.persons.remove(person)
                        personToArchieve.value = person
                    }

                    SwipeToDismiss(
                        state = dismissState,
                        directions = setOf(
                            DismissDirection.EndToStart,
                            DismissDirection.StartToEnd
                        ),
                        background = {

                            val backgroundColor by animateColorAsState(
                                when (dismissState.targetValue) {
                                    DismissValue.DismissedToStart -> Color.Red.copy(alpha = 0.8f)
                                    DismissValue.DismissedToEnd -> Color.Green.copy(alpha = 0.8f)
                                    else -> Color.White
                                }, label = "color"
                            )

                            val iconImageVector = when (dismissState.targetValue) {
                                DismissValue.DismissedToEnd -> Icons.Outlined.AddCircle
                                else -> Icons.Outlined.Delete
                            }

                            val iconAlignment = when (dismissState.targetValue) {
                                DismissValue.DismissedToEnd -> Alignment.CenterStart
                                else -> Alignment.CenterEnd
                            }

                            val iconScale by animateFloatAsState(
                                targetValue = if (dismissState.targetValue == DismissValue.Default) 0.3f else 1.3f,
                                label = "icon"
                            )

                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(color = backgroundColor)
                                    .padding(horizontal = 10.dp),

                                contentAlignment = iconAlignment
                            ) {
                                Icon(
                                    modifier = Modifier.scale(iconScale),
                                    imageVector = iconImageVector,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        },
                        dismissContent = {
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Text(text = index.toString(), modifier = Modifier.padding(5.dp))
                                Text(
                                    text = person.name,
                                    modifier = Modifier
                                        .fillMaxSize(.9f)
                                        .padding(12.dp)
                                )
                                Text(person.age.toString())
                            }
                        }
                    )
                }
            }
        }
    )

    LaunchedEffect(personToArchieve.value) {
        if (personToArchieve.value != null) {
            val name = personToArchieve.value?.name
            personToArchieve.value = null
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Archieved " + name,
                    actionLabel = "Hide",
                    duration = SnackbarDuration.Short
                )
                personToArchieve.value = null
                when (result) {
                    SnackbarResult.Dismissed -> {}
                    SnackbarResult.ActionPerformed -> {
                    }
                }
            }
        }
    }
}

data class Person(val id: Int, val name: String, val age: Int)