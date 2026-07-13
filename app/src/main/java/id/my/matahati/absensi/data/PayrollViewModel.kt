package id.my.matahati.absensi.data

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.matahati.absensi.SessionManager
import kotlinx.coroutines.launch

class PayrollViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val session = SessionManager(application)

    var payrolls by mutableStateOf<List<PayrollItem>>(
        emptyList()
    )

    var loading by mutableStateOf(false)
    var departments by mutableStateOf<List<DepartmentItem>>(
        emptyList()
    )

    var selectedDepartment by mutableStateOf<String?>(null)

    var selectedYear by mutableStateOf(2026)

    var selectedMonth by mutableStateOf(5)
    var selectedPayroll by mutableStateOf<PayrollDetail?>(
        null
    )

    var searchQuery by mutableStateOf("")

    var loadingDetail by mutableStateOf(false)

    var updating by mutableStateOf(false)

    var updateSuccess by mutableStateOf(false)

    var errorMessage by mutableStateOf<String?>(null)

    fun loadDepartments() {

        val userId = session.getUserId()

        viewModelScope.launch {

            try {

                Log.d("PAYROLL", "Session userId = $userId")

                val response =
                    RetrofitClientLaravel.instance
                        .getDepartments(userId)

                Log.d(
                    "PAYROLL",
                    "department code = ${response.code()}"
                )

                Log.d(
                    "PAYROLL",
                    "department body = ${response.body()}"
                )

                if (response.isSuccessful) {

                    departments =
                        response.body()?.data
                            ?: emptyList()

                    Log.d(
                        "PAYROLL",
                        "total department = ${departments.size}"
                    )

                    /* ================= DEFAULT DEPT ================= */

                    if (
                        selectedDepartment == null &&
                        departments.isNotEmpty()
                    ) {

                        selectedDepartment =
                            departments.first().nid.toString()

                        loadPayrolls()
                    }

                } else {

                    Log.e(
                        "PAYROLL",
                        "Failed load departments"
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                Log.e(
                    "PAYROLL",
                    "Department Exception = ${e.message}"
                )
            }
        }
    }

    /* =======================================================
     * LOAD PAYROLL LIST
     * ======================================================= */

    fun loadPayrolls() {

        val userId = session.getUserId()

        viewModelScope.launch {

            loading = true

            errorMessage = null

            try {

                Log.d(
                    "PAYROLL",
                    "Load payroll start"
                )

                Log.d(
                    "PAYROLL",
                    "department = $selectedDepartment"
                )

                Log.d(
                    "PAYROLL",
                    "year = $selectedYear"
                )

                Log.d(
                    "PAYROLL",
                    "month = $selectedMonth"
                )

                Log.d("PAYROLL", "Session userId = $userId")

                val response =
                    RetrofitClientLaravel.instance
                        .getPayrollList(
                            userId = userId,
                            departmentId = selectedDepartment,
                            year = selectedYear,
                            month = selectedMonth
                        )

                Log.d(
                    "PAYROLL",
                    "code = ${response.code()}"
                )

                Log.d(
                    "PAYROLL",
                    "body = ${response.body()}"
                )

                if (response.isSuccessful) {

                    payrolls =
                        response.body()?.data
                            ?: emptyList()

                    Log.d(
                        "PAYROLL",
                        "total payroll = ${payrolls.size}"
                    )

                } else {

                    errorMessage =
                        "Gagal load payroll (${response.code()})"

                    Log.e(
                        "PAYROLL",
                        errorMessage ?: "Unknown error"
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                errorMessage =
                    e.message ?: "Unknown error"

                Log.e(
                    "PAYROLL",
                    "Payroll Exception = ${e.message}"
                )
            }

            loading = false
        }
    }

    /* =======================================================
     * LOAD DETAIL PAYROLL
     * ======================================================= */

    fun loadPayrollDetail(id: Int) {

        viewModelScope.launch {

            loadingDetail = true

            errorMessage = null

            try {

                Log.d(
                    "PAYROLL",
                    "Load detail payroll id = $id"
                )

                val response =
                    RetrofitClientLaravel.instance
                        .getPayrollDetail(id)

                Log.d(
                    "PAYROLL",
                    "detail code = ${response.code()}"
                )

                Log.d(
                    "PAYROLL",
                    "detail body = ${response.body()}"
                )

                if (response.isSuccessful) {

                    selectedPayroll =
                        response.body()?.data

                } else {

                    errorMessage =
                        "Gagal load detail payroll"

                    Log.e(
                        "PAYROLL",
                        errorMessage ?: "Unknown error"
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                errorMessage =
                    e.message

                Log.e(
                    "PAYROLL",
                    "Detail Exception = ${e.message}"
                )
            }

            loadingDetail = false
        }
    }

    /* =======================================================
     * UPDATE PAYROLL
     * ======================================================= */

    fun updatePayroll(
        id: Int,
        request: PayrollUpdateRequest,
        onSuccess: () -> Unit = {}
    ) {

        viewModelScope.launch {

            updating = true

            errorMessage = null

            updateSuccess = false

            try {

                Log.d(
                    "PAYROLL",
                    "Update payroll id = $id"
                )

                val response =
                    RetrofitClientLaravel.instance
                        .updatePayroll(
                            id = id,
                            request = request
                        )

                Log.d(
                    "PAYROLL",
                    "update code = ${response.code()}"
                )

                Log.d(
                    "PAYROLL",
                    "update body = ${response.body()}"
                )

                if (response.isSuccessful) {

                    updateSuccess = true

                    onSuccess()

                    loadPayrolls()

                } else {

                    errorMessage =
                        "Gagal update payroll (${response.code()})"

                    Log.e(
                        "PAYROLL",
                        errorMessage ?: "Unknown error"
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                errorMessage =
                    e.message ?: "Unknown error"

                Log.e(
                    "PAYROLL",
                    "Update Exception = ${e.message}"
                )
            }

            updating = false
        }
    }

    val filteredPayrolls: List<PayrollItem>
        get() {

            if (searchQuery.isBlank()) {
                return payrolls
            }

            return payrolls.filter {

                (it.user_name ?: "")
                    .contains(
                        searchQuery,
                        ignoreCase = true
                    )
            }
        }

    /* =======================================================
     * CHANGE FILTER
     * ======================================================= */

    fun setDepartment(department: String?) {

        selectedDepartment = department

        loadPayrolls()
    }

    fun setYear(year: Int) {

        selectedYear = year

        loadPayrolls()
    }

    fun setMonth(month: Int) {

        selectedMonth = month

        loadPayrolls()
    }
}