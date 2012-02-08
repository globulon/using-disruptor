package com.promindis.disruptor.port

import org.specs2.mutable.Specification

/**
 * Date: 08/02/12
 * Time: 14:15
 */

final class PaddedLongSpecification extends Specification {

  "Padded Long " should {
    "provide access to embedded value" in {
      new PaddedLong(7L).value.should(beEqualTo(7L))
    }

    "allow to sum up padded values" in {
      new PaddedLong(7L).sumPaddingToPreventOptimisation().should(beEqualTo(7L))
    }


  }

}
