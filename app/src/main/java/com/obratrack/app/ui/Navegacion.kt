package com.obratrack.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.obratrack.app.ui.screens.EtapaFormScreen
import com.obratrack.app.ui.screens.GastoFormScreen
import com.obratrack.app.ui.screens.ObraFormScreen
import com.obratrack.app.ui.screens.ObraScreen
import com.obratrack.app.ui.screens.ObrasScreen
import com.obratrack.app.ui.screens.TrabajadorFormScreen

/** Rutas de la aplicación. El 0 en un identificador significa "registro nuevo". */
object Rutas {
    const val OBRAS = "obras"
    fun obraForm(obraId: Long) = "obra_form/$obraId"
    fun obra(obraId: Long) = "obra/$obraId"
    fun etapaForm(obraId: Long, etapaId: Long) = "etapa_form/$obraId/$etapaId"
    fun trabajadorForm(obraId: Long, trabajadorId: Long) = "trabajador_form/$obraId/$trabajadorId"
    fun gastoForm(obraId: Long, gastoId: Long) = "gasto_form/$obraId/$gastoId"
}

private val argumentoObra = navArgument("obraId") { type = NavType.LongType }

@Composable
fun ObraTrackNav() {
    val nav = rememberNavController()
    val vm: ObraViewModel = viewModel()
    val volver: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = Rutas.OBRAS) {

        composable(Rutas.OBRAS) {
            ObrasScreen(
                vm = vm,
                onNueva = { nav.navigate(Rutas.obraForm(0)) },
                onAbrir = { id -> nav.navigate(Rutas.obra(id)) }
            )
        }

        composable("obra_form/{obraId}", arguments = listOf(argumentoObra)) { entry ->
            val obraId = entry.arguments?.getLong("obraId") ?: 0L
            ObraFormScreen(vm = vm, obraId = obraId, onVolver = volver)
        }

        composable("obra/{obraId}", arguments = listOf(argumentoObra)) { entry ->
            val obraId = entry.arguments?.getLong("obraId") ?: 0L
            ObraScreen(
                vm = vm,
                obraId = obraId,
                onVolver = volver,
                navegar = { ruta -> nav.navigate(ruta) }
            )
        }

        composable(
            "etapa_form/{obraId}/{etapaId}",
            arguments = listOf(argumentoObra, navArgument("etapaId") { type = NavType.LongType })
        ) { entry ->
            val obraId = entry.arguments?.getLong("obraId") ?: 0L
            val etapaId = entry.arguments?.getLong("etapaId") ?: 0L
            EtapaFormScreen(vm = vm, obraId = obraId, etapaId = etapaId, onVolver = volver)
        }

        composable(
            "trabajador_form/{obraId}/{trabajadorId}",
            arguments = listOf(argumentoObra, navArgument("trabajadorId") { type = NavType.LongType })
        ) { entry ->
            val obraId = entry.arguments?.getLong("obraId") ?: 0L
            val trabajadorId = entry.arguments?.getLong("trabajadorId") ?: 0L
            TrabajadorFormScreen(vm = vm, obraId = obraId, trabajadorId = trabajadorId, onVolver = volver)
        }

        composable(
            "gasto_form/{obraId}/{gastoId}",
            arguments = listOf(argumentoObra, navArgument("gastoId") { type = NavType.LongType })
        ) { entry ->
            val obraId = entry.arguments?.getLong("obraId") ?: 0L
            val gastoId = entry.arguments?.getLong("gastoId") ?: 0L
            GastoFormScreen(vm = vm, obraId = obraId, gastoId = gastoId, onVolver = volver)
        }
    }
}
