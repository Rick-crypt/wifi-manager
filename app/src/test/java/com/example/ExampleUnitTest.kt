package com.example

import com.example.data.entity.FieldDefinitionEntity
import com.example.data.model.FieldType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `test field options parsing`() {
    val field = FieldDefinitionEntity(
      id = 1,
      key = "nationality",
      label = "Nationalité",
      type = FieldType.SELECT.name,
      isRequired = true,
      isAuthKey = true,
      optionsJson = "[\"Gabonaise\", \"Camerounaise\", \"Congolaise\"]"
    )

    val options = field.getOptionsList()
    assertEquals(3, options.size)
    assertTrue(options.contains("Gabonaise"))
    assertTrue(options.contains("Camerounaise"))
    assertTrue(options.contains("Congolaise"))
  }
}
