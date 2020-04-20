package jp.buzza.androidgde.extension

/*
 Q: When we need you Generic type
 A: When we are not sure about data type will use
 T is generic template
 subclass la lop con
 val integer: Int = 1
 val number: Number = integer
 We can do it because Int is subclass of Number
 Nhung khong lam nguoc lai duoc .
 */
class GenericsExample<T> (input: T) {
    init {
        println("Inputted $input")
    }
}

fun main(args: Array<String>) {
    val nCheck: Number
    val value1 = GenericsExample("Fuck you")
    val value2 = GenericsExample(10)
}
/*
SuperType có thể được gán cho SubType, sử dụng in
SubType có thể được gán cho SuperType, sử dụng out
https://medium.com/@elye.project/in-and-out-type-variant-of-kotlin-587e4fa2944c
*/
