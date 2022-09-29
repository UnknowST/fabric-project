package main

import (
	"encoding/json"
	"fmt"
	"github.com/golang/protobuf/ptypes"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"time"
)

// 证书结构体
type Cerinfo struct {
	Num      string `json:"num"`
	Username string `json:"username"`
	Name     string `json:"name"`
	Title    string `json:"title"`
	Hash     string `json:"hash"`
}

// 证书历史记录结构体
type CerinfoHistory struct {
	Record    *Cerinfo  `json:"record"`
	TxId      string    `json:"txId"`
	Timestamp time.Time `json:"timestamp"`
	IsDelete  bool      `json:"isDelete"`
}

// 查询所有的信息
type CerinfoList struct {
	Key    string   `json:"Key"`
	Record *Cerinfo `json:"Record"`
}

// SmartContract provides functions for managing a car
type SmartContract struct {
	contractapi.Contract
}

// 返回交易ID和时间戳
type TxidAndTime struct {
	Number    string    `json:"number"`
	Txid      string    `json:"txid"`
	Timestamp time.Time `json:"timestamp"`
}

// 初始化账本 添加数据
func (s *SmartContract) InitLedger(ctx contractapi.TransactionContextInterface) error {

	infos := []Cerinfo{
		Cerinfo{Num: "10000001", Username: "100004", Name: "小钱", Title: "测试数据", Hash: "asdfasdfasdf"},
	}
	for _, info := range infos {
		userAsBytes, _ := json.Marshal(info)
		err := ctx.GetStub().PutState(info.Num, userAsBytes)

		if err != nil {
			return fmt.Errorf("Failed to put to world state." + err.Error())
		}
	}
	return nil
}

// 添加数据进账本
func (s *SmartContract) AddInfo(ctx contractapi.TransactionContextInterface, num string, username string, name string, title string, hash string) error {
	info := Cerinfo{
		Num:      num,
		Username: username,
		Name:     name,
		Title:    title,
		Hash:     hash,
	}
	infoAsBytes, err := ctx.GetStub().GetState(num)
	if err != nil {
		return fmt.Errorf("Failed to put to world state." + err.Error())
	}
	if infoAsBytes != nil {
		return fmt.Errorf("num %s already exist.", num)
	}
	info1AsBytes, _ := json.Marshal(info)
	return ctx.GetStub().PutState(num, info1AsBytes)

}

// 查找某个存证
func (s *SmartContract) QueryInfo(ctx contractapi.TransactionContextInterface, num string) (*Cerinfo, error) {

	infoAsBytes, err := ctx.GetStub().GetState(num)
	if err != nil {
		return nil, fmt.Errorf("Failed to put to world state." + err.Error())
	}
	if infoAsBytes == nil {
		return nil, fmt.Errorf("num %s dose not exist.", num)

	}
	info := new(Cerinfo)
	_ = json.Unmarshal(infoAsBytes, info)
	return info, nil
}

//删除某一个存证
func (s *SmartContract) DeleteInfo(ctx contractapi.TransactionContextInterface, num string) error {

	infoAsBytes, err := ctx.GetStub().GetState(num)
	if err != nil {
		return fmt.Errorf("Failed to put to world state." + err.Error())
	}
	if infoAsBytes == nil {
		return fmt.Errorf("num %s dose not exist.", num)
	}
	return ctx.GetStub().DelState(num)
}

//查询所有存证数据
func (s *SmartContract) QueryAllInfo(ctx contractapi.TransactionContextInterface) ([]CerinfoList, error) {

	startKey := ""
	endKey := ""
	resultsIterator, err := ctx.GetStub().GetStateByRange(startKey, endKey)
	if err != nil {
		return nil, fmt.Errorf("Failed to put to world state." + err.Error())
	}
	defer resultsIterator.Close()
	results := []CerinfoList{}
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()

		if err != nil {
			return nil, fmt.Errorf("Failed to put to world state." + err.Error())
		}

		info := new(Cerinfo)
		_ = json.Unmarshal(queryResponse.Value, info)

		queryResult := CerinfoList{Key: queryResponse.Key, Record: info}
		results = append(results, queryResult)
	}
	return results, nil
}

//查询某一存证的历史记录
func (s *SmartContract) GetInfoHistory(ctx contractapi.TransactionContextInterface, num string) ([]CerinfoHistory, error) {
	resultIterator, err := ctx.GetStub().GetHistoryForKey(num)
	if err != nil {
		return nil, err
	}
	defer resultIterator.Close()
	var records []CerinfoHistory
	for resultIterator.HasNext() {
		response, err := resultIterator.Next()
		if err != nil {
			return nil, err
		}
		var info Cerinfo
		if len(response.Value) > 0 {
			err = json.Unmarshal(response.Value, &info)
			if err != nil {
				return nil, err
			}
		} else {
			info = Cerinfo{
				Num: num,
			}
		}
		timestamp, err := ptypes.Timestamp(response.Timestamp)

		if err != nil {
			return nil, err
		}

		record := CerinfoHistory{
			TxId:      response.TxId,
			Timestamp: timestamp,
			Record:    &info,
			IsDelete:  response.IsDelete,
		}
		records = append(records, record)
	}

	return records, nil
}

// 返回最新的存证编号
func (s *SmartContract) GetNum(ctx contractapi.TransactionContextInterface) (string, error) {
	result, err := s.QueryAllInfo(ctx)
	if err != nil {
		return "", fmt.Errorf(err.Error())
	}
	// 按key 排序 这里用的是冒泡排序 可能会比较慢
	for i := 1; i < len(result); i++ {
		if result[i].Key < result[i-1].Key {
			cinfo := CerinfoList{
				Key:    result[i].Key,
				Record: result[i].Record,
			}
			result[i].Key = result[i-1].Key
			result[i].Record = result[i-1].Record
			result[i-1].Key = cinfo.Key
			result[i-1].Record = cinfo.Record
		}
	}
	if len(result) == 0 {
		return "10000000", nil
	}
	return result[len(result)-1].Key, nil
}

// 返回某一个账号的存证列表
func (s *SmartContract) GetByUsername(ctx contractapi.TransactionContextInterface, username string) ([]CerinfoList, error) {
	result, err := s.QueryAllInfo(ctx)
	if err != nil {
		return nil, fmt.Errorf(err.Error())
	}
	var result2 []CerinfoList
	for _, value := range result {
		if value.Record.Username == username {
			info := CerinfoList{
				Key:    value.Key,
				Record: value.Record,
			}
			result2 = append(result2, info)
		}
	}
	return result2, nil
}

// 根据编号查询某一个存证是否存在
func (s *SmartContract) IsExist(ctx contractapi.TransactionContextInterface, num string) (bool, error) {
	result, err := ctx.GetStub().GetState(num)
	if err != nil {
		return false, fmt.Errorf(err.Error())
	}
	return result != nil, nil
}

// 返回某一个交易的 交易ID和时间戳
func (s *SmartContract) GetTxidAndtime(ctx contractapi.TransactionContextInterface, num string) (TxidAndTime, error) {
	res, err := s.GetInfoHistory(ctx, num)
	var txi TxidAndTime
	if err != nil {
		// 对于 返回的txi 自己判断 是否为空
		return txi, fmt.Errorf(err.Error())
	}
	if res == nil {
		return txi, fmt.Errorf("num %s dose not exist.", num)
	}

	for _, value := range res {
		if value.IsDelete == false {
			txi = TxidAndTime{
				Number:    num,
				Txid:      value.TxId,
				Timestamp: value.Timestamp,
			}

		}
	}
	return txi, nil
}

func main() {

	chainCode, err := contractapi.NewChaincode(new(SmartContract))

	if err != nil {
		fmt.Printf("Error create certificate chainCode: %s", err.Error())
		return
	}

	if err := chainCode.Start(); err != nil {
		fmt.Printf("Error starting certificate chainCode: %s", err.Error())
	}
}
