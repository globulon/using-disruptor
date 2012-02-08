package com.promindis.disruptor.genuine

import org.specs2.mutable.Specification
import com.lmax.disruptor.util.PaddedLong

/**
 * Date: 08/02/12
 * Time: 14:15
 */

final class PaddedLongSpecification extends Specification{

  "Padded Long " should {
    "provide access to embedded value" in {
      new PaddedLong(7L).get().should(beEqualTo(7L))
    }

    "allow to sum up padded values" in {
      new PaddedLong(7L).sumPaddingToPreventOptimisation().should(beEqualTo(7L))
    }


  }

}
