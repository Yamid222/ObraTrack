package com.obratrack.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Categoría de gasto creada por el usuario. No pertenece a una obra: sirve para todas. */
@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val argb: Long
)

/** Categoría lista para mostrar, sea de las fijas o de las creadas por el usuario. */
data class CategoriaVista(
    val clave: String,   // lo que se guarda en Gasto.categoria
    val etiqueta: String,
    val argb: Long
)

object Categorias {
    /** Colores que se van asignando a las categorías nuevas. */
    private val colores = listOf(
        0xFF0891B2, 0xFFDB2777, 0xFF65A30D, 0xFFCA8A04, 0xFF4F46E5, 0xFF0D9488, 0xFF9333EA, 0xFFB45309
    )

    fun colorPara(cantidadExistente: Int): Long = colores[cantidadExistente % colores.size]

    fun clave(categoria: Categoria): String = "C${categoria.id}"

    /** Las cuatro categorías fijas seguidas de las creadas por el usuario. */
    fun todas(propias: List<Categoria>): List<CategoriaVista> =
        CategoriaGasto.entries.map { CategoriaVista(it.name, it.etiqueta, it.argb) } +
            propias.map { CategoriaVista(clave(it), it.nombre, it.argb) }

    /** Busca la categoría de un gasto; si no existe se muestra como "Otros". */
    fun buscar(clave: String, todas: List<CategoriaVista>): CategoriaVista =
        todas.firstOrNull { it.clave == clave }
            ?: CategoriaGasto.OTROS.let { CategoriaVista(it.name, it.etiqueta, it.argb) }
}
