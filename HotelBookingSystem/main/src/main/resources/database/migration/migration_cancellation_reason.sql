-- ============================================================
-- Migration: Cập nhật CancellationReason enum values trong DB
-- Database: Microsoft SQL Server
-- Bảng: bookings (cột: cancellation_reason)
-- Ngày: 2026-03-09
-- ============================================================

BEGIN TRANSACTION;

BEGIN TRY

    -- 1. PLAN_CHANGED → CHANGE_OF_PLANS
UPDATE bookings
SET cancellation_reason = 'CHANGE_OF_PLANS'
WHERE cancellation_reason = 'PLAN_CHANGED';

PRINT N'✔ PLAN_CHANGED → CHANGE_OF_PLANS: ' + CAST(@@ROWCOUNT AS NVARCHAR) + N' row(s) updated';

    -- 2. FIND_BETTER_PRICE → FOUND_ALTERNATIVE
UPDATE bookings
SET cancellation_reason = 'FOUND_ALTERNATIVE'
WHERE cancellation_reason = 'FIND_BETTER_PRICE';

PRINT N'✔ FIND_BETTER_PRICE → FOUND_ALTERNATIVE: ' + CAST(@@ROWCOUNT AS NVARCHAR) + N' row(s) updated';

    -- 3. WORK_EMERGENCY → WORK_COMMITMENT
UPDATE bookings
SET cancellation_reason = 'WORK_COMMITMENT'
WHERE cancellation_reason = 'WORK_EMERGENCY';

PRINT N'✔ WORK_EMERGENCY → WORK_COMMITMENT: ' + CAST(@@ROWCOUNT AS NVARCHAR) + N' row(s) updated';

    -- 4. NO_REASON → OTHER
UPDATE bookings
SET cancellation_reason = 'OTHER'
WHERE cancellation_reason = 'NO_REASON';

PRINT N'✔ NO_REASON → OTHER: ' + CAST(@@ROWCOUNT AS NVARCHAR) + N' row(s) updated';

    -- 5. PERSONAL_REASON — giữ nguyên (không đổi tên)
    -- 6. HEALTH_ISSUE    — giữ nguyên (không đổi tên)

    -- ============================================================
    -- Kiểm tra kết quả sau migration
    -- ============================================================
    PRINT N'';
    PRINT N'========== KẾT QUẢ SAU MIGRATION ==========';

SELECT
    cancellation_reason,
    COUNT(*) AS total
FROM bookings
WHERE cancellation_reason IS NOT NULL
GROUP BY cancellation_reason
ORDER BY total DESC;

-- Kiểm tra còn giá trị cũ nào sót không
IF EXISTS (
        SELECT 1 FROM bookings
        WHERE cancellation_reason IN ('PLAN_CHANGED', 'FIND_BETTER_PRICE', 'WORK_EMERGENCY', 'NO_REASON')
    )
BEGIN
        PRINT N'⚠ CẢNH BÁO: Vẫn còn giá trị enum cũ chưa được migrate!';
        RAISERROR(N'Migration không hoàn tất — còn giá trị cũ trong DB', 16, 1);
END
ELSE
BEGIN
        PRINT N'✔ Migration hoàn tất — không còn giá trị enum cũ.';
END

COMMIT TRANSACTION;
PRINT N'✔ COMMIT thành công.';

END TRY
BEGIN CATCH

ROLLBACK TRANSACTION;

    PRINT N'✘ ĐÃ ROLLBACK — Có lỗi xảy ra:';
    PRINT N'  Error: ' + ERROR_MESSAGE();
    PRINT N'  Line : ' + CAST(ERROR_LINE() AS NVARCHAR);

END CATCH;