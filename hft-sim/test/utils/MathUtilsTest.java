package utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

public class MathUtilsTest {
	
	@Test
	public void testingLogn() {
		
		//standard inputs for logn
		int number= 10;
		int base = 10;

		int result = MathUtils.logn(number, base);
		assertEquals(1, result);//normal test case

		result = MathUtils.logn(number, base);
		assertEquals(1, result);//normal test case

		//log base 10 of 10 is 1
		number = 10;
		base = 10;
		result = MathUtils.logn(number, base);
		assertEquals(1, result);
		
		//boundary cases of number <= 0, as ln is undefined for those values
		//these ensure -1 is given where log n is undefined
		number = 0;
		base = 10;
		result = MathUtils.logn(number, base);
		assertEquals(-1, result);
		number=-999;
		result = MathUtils.logn(number,base);
		assertEquals(-1, result);
		
		//Pathological test cases like
		//number = .000000000000001; wouldn't apply as number is of type int
		
		//shows that logn function provided allows use of negative base values
		number = 10;
		base = -1; 
		result = MathUtils.logn(number, base);
		// 10/-1=-10, log =0, hence result is 0
		//it appears that in general, if the base is negative while the number is positive
		// result returned will be 0
		assertEquals(0, result);
		
		number = -2;
		result = MathUtils.logn(number,base);
		//as number provided is already negative, would not enter loop, returning -1
		assertEquals(-1, result);
		
		//show -1 is given if and only if starting number is already negative or zero
		number = 0;
		base = 2;
		result = MathUtils.logn(number, base);
		assertEquals(-1, result);
		number = -98;
		result = MathUtils.logn(number,base);
		assertEquals(-1, result);
		
		number = 5; base = 3;
		//if using double answer should be  0.69897000434
		result = MathUtils.logn(number,base);
		//however integer math forces result to be rounded to 1
		assertEquals(1, result);
		
		//7 log 9 is   0.88562187458
		number = 7; 
		base = 9;
		result = MathUtils.logn(number,base);
		//as 7 > 0, 7/9 = 0.7778.. gets cut to 0 
		//stopping the incrementing of log at 0
		assertEquals(0, result);
		
		//62 log e is  4.127134385,
		//this is rounded due to repeated integer division to 3
		number = 62; 
		base = 3;//rounding as input is integer base
		result = MathUtils.logn(number,base);
		//62/3=20.666, this is cut/truncated to 20, 
		//20/3 cut to 6, 6/3 is 2, 2/3 is cut to 0
		//resulting in result of -1+1(4) = 3
		//example of cases in which answer differs from double variant by more than 1 
		assertEquals(3, result);
		
		//39 log 37 is  1.591064607 
		number =39; 
		base =37;
		result = MathUtils.logn(number,base);
		// results of repeated integer divisions, result in  1.59 being truncated to 1
		assertEquals(1, result);
	}

	@Test
	public void testingQuantizeInt()
	{ 	
		//standard input
		int n = 1001;
		int quanta= 10;
		int q = MathUtils.quantize(n, quanta);
		assertEquals(1000, q);
		
		//used to verify function rounds to positive infinity for negative numbers
		n = -32; 
		q = MathUtils.quantize(n, quanta);
		assertEquals(-30, q);
		
		//verifies that when provided a value that is a multiple of the quanta
		//the program will round to the same value
		n=0;
		q = MathUtils.quantize(n, quanta);
		assertEquals(0, q);
	}
	
	@Test
	public void testingQuantizeDouble()
	{ 	
		double eps = 0.00001;
		//standard form of input for decimal variant of quantize function
		double n = 100.003;
		double quanta= 10;
		double qresult = MathUtils.quantize(n, quanta);
		assertEquals(100, qresult, eps);
		
		//verifies that n is not rounded incorrectly
		//verifies that  negative numbers below middle round up to quanta closer to 0
		n = -34.99999; 
		qresult = MathUtils.quantize(n, quanta);
		assertEquals(-30, qresult, eps);
		
		//in cases where the double n is closer to the lower quanta, quantize rounds down to lower quanta
		n=-35.999999;
		qresult = MathUtils.quantize(n, quanta);
		assertEquals(-40, qresult, eps);

		//checking that  when provided a value that is a multiple of the quanta
		//the program will round to the same value
		n=-0.000000000000000000;
		qresult = MathUtils.quantize(n, quanta);
		assertEquals(0, qresult, eps);
		//assertTrue(n==0)// also can be used to check floating point accuracy
	}

	@Test
	public void testingBound()
	{ 
		//testing basic functionality of bound function
		//verifies minimizes num and upper
		//maximizes result and lower bound
		int num = 0;
		int lower = 1;
		int upper = 2;
		int b = MathUtils.bound(num,lower, upper);
		assertEquals(1, b);
		
		//testing valid results using repeated values 
		// for variables
		num=0; 
		lower = 0;
		upper = 1;
		b = MathUtils.bound(num,lower, upper);
		assertEquals(0, b);
		
		num=0; lower = 1; upper =0;
		b = MathUtils.bound(num,lower, upper);
		assertNotSame(0, b);
		
		num=0; lower = 0; upper =1;
		b = MathUtils.bound(num,lower, upper);
		assertEquals(0, b);
		
		//verifying case when all three inputs provided are same
		num = 0;
		lower = 0;
		upper = 0;
		b = MathUtils.bound(num,lower, upper);
		assertEquals(0, b);
		
		//testing with negative numbers and large values provided
		num = -999999899;
		//noting that this is the max length int num can have before IDE throws an error 
		lower = -88;
		upper = -875;
		b = MathUtils.bound(num,lower, upper);
		assertEquals(-88, b);
		
		//testing using inputs for largest integer values possible
		int numLargest =  2147483647; //testing with likely largest number storable in Java x32
		num = numLargest;
		lower = -num; //using smallest number possible in Java
		upper = num;
		b = MathUtils.bound(num,lower, upper);
		assertEquals(num, b);
		upper = -numLargest;
		b = MathUtils.bound(num,lower, upper);
		assertEquals(upper, b);
	}
}
