SELECT
    pat.Name
FROM
    DataSet d,
    Pathway pat
WHERE
    d.Name=?
    && d.WID=pat.DataSetWID
    && pat.WID=?