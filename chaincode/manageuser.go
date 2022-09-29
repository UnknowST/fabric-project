package main

import (
	"encoding/json"
	"fmt"
	"github.com/golang/protobuf/ptypes"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"time"
)

// SmartContract provides functions for managing a car
type SmartContract struct {
	contractapi.Contract
}

// user info
type User struct {
	Username string `json:"username"`
	Password string `json:"password"`
	Name     string `json:"name"`
	Email    string `json:"email"`
}

// HistoryQueryResult structure used for returning result of history query
type HistoryQueryResult struct {
	Record    *User     `json:"record"`
	TxId      string    `json:"txId"`
	Timestamp time.Time `json:"timestamp"`
	IsDelete  bool      `json:"isDelete"`
}

// QueryResult structure used for handling result of query
type QueryResult struct {
	Key    string `json:"Key"`
	Record *User
}

// 交易ID和时间戳
type TxidAndTime struct {
	Number    string    `json:"number"`
	Txid      string    `json:"txid"`
	Timestamp time.Time `json:"timestamp"`
}

// 初始化账本 添加数据
func (s *SmartContract) InitLedger(ctx contractapi.TransactionContextInterface) error {

	users := []User{
		User{Username: "100001", Name: "小钱", Password: "测试账号", Email: "1757949943@qq.com"},
	}
	for _, user := range users {
		userAsBytes, _ := json.Marshal(user)
		err := ctx.GetStub().PutState(user.Username, userAsBytes)

		if err != nil {
			return fmt.Errorf("Failed to put to world state." + err.Error())
		}
	}
	return nil
}

//查询所有用户信息
func (s *SmartContract) QueryAllUser(ctx contractapi.TransactionContextInterface) ([]QueryResult, error) {

	startKey := ""
	endKey := ""
	resultsIterator, err := ctx.GetStub().GetStateByRange(startKey, endKey)
	if err != nil {
		return nil, fmt.Errorf("Failed to put to world state." + err.Error())
	}
	defer resultsIterator.Close()
	results := []QueryResult{}
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()

		if err != nil {
			return nil, fmt.Errorf("Failed to put to world state." + err.Error())
		}

		user := new(User)
		_ = json.Unmarshal(queryResponse.Value, user)

		queryResult := QueryResult{Key: queryResponse.Key, Record: user}
		results = append(results, queryResult)
	}
	return results, nil
}

// 注册用户
func (s *SmartContract) AddUser(ctx contractapi.TransactionContextInterface, username string, password string, name string, email string) error {

	user := User{
		Username: username,
		Name:     name,
		Password: password,
		Email:    email,
	}
	userAsBytes, err := ctx.GetStub().GetState(username)
	if err != nil {
		return fmt.Errorf("Failed to put to world state." + err.Error())
	}
	if userAsBytes != nil {
		return fmt.Errorf("username %s already exist.", username)
	}
	user1AsBytes, _ := json.Marshal(user)

	return ctx.GetStub().PutState(username, user1AsBytes)

}

// 查询某一个用户信息
func (s *SmartContract) QueryUser(ctx contractapi.TransactionContextInterface, username string) (*User, error) {

	userAsBytes, err := ctx.GetStub().GetState(username)
	if err != nil {
		return nil, fmt.Errorf("Failed to put to world state." + err.Error())
	}
	if userAsBytes == nil {
		return nil, fmt.Errorf("username %s dose not exist.", username)

	}
	user := new(User)
	_ = json.Unmarshal(userAsBytes, user)
	return user, nil
}

//修改密码
func (s *SmartContract) ModifPassw(ctx contractapi.TransactionContextInterface, username string, password string) error {

	userAsBytes, err := ctx.GetStub().GetState(username)
	if err != nil {
		return fmt.Errorf("Failed to put to world state." + err.Error())
	}
	if userAsBytes == nil {
		return fmt.Errorf("username %s dose not exist.", username)
	}
	user := new(User)
	_ = json.Unmarshal(userAsBytes, user)
	user.Password = password
	user1AsBytes, _ := json.Marshal(user)
	return ctx.GetStub().PutState(username, user1AsBytes)
}

// 修改用户信息 暂时限制为只能修改 email
func (s *SmartContract) ModifEmail(ctx contractapi.TransactionContextInterface, username string, email string) error {

	userAsBytes, err := ctx.GetStub().GetState(username)
	if err != nil {
		return fmt.Errorf("Failed to put to world state." + err.Error())
	}
	if userAsBytes == nil {
		return fmt.Errorf("username %s dose not exist.", username)
	}
	user := new(User)
	_ = json.Unmarshal(userAsBytes, user)
	user.Email = email
	user1AsBytes, _ := json.Marshal(user)
	return ctx.GetStub().PutState(username, user1AsBytes)
}

//删除用户
func (s *SmartContract) DeleteUser(ctx contractapi.TransactionContextInterface, username string) error {

	userAsBytes, err := ctx.GetStub().GetState(username)
	if err != nil {
		return fmt.Errorf("Failed to put to world state." + err.Error())
	}
	if userAsBytes == nil {
		return fmt.Errorf("username %s dose not exist.", username)
	}
	return ctx.GetStub().DelState(username)
}

// 查询某一个账号的历史记录
func (s *SmartContract) GetUserHistory(ctx contractapi.TransactionContextInterface, username string) ([]HistoryQueryResult, error) {
	resultIterator, err := ctx.GetStub().GetHistoryForKey(username)
	if err != nil {
		return nil, err
	}
	defer resultIterator.Close()
	var records []HistoryQueryResult
	for resultIterator.HasNext() {
		response, err := resultIterator.Next()
		if err != nil {
			return nil, err
		}
		var user User
		if len(response.Value) > 0 {
			err = json.Unmarshal(response.Value, &user)
			if err != nil {
				return nil, err
			}
		} else {
			user = User{
				Username: username,
			}
		}
		timestamp, err := ptypes.Timestamp(response.Timestamp)
		if err != nil {
			return nil, err
		}

		record := HistoryQueryResult{
			TxId:      response.TxId,
			Timestamp: timestamp,
			Record:    &user,
			IsDelete:  response.IsDelete,
		}
		records = append(records, record)
	}

	return records, nil
}

// 检查某一账号是否存在
func (s *SmartContract) IsExist(ctx contractapi.TransactionContextInterface, username string) (bool, error) {
	result, err := ctx.GetStub().GetState(username)
	if err != nil {
		return false, fmt.Errorf(err.Error())
	}
	return result != nil, nil
}

//返回某一个账号的创建交易ID和时间戳
func (s *SmartContract) GetTxidAndtime(ctx contractapi.TransactionContextInterface, username string) (TxidAndTime, error) {
	result, err := s.GetUserHistory(ctx, username)
	var txi TxidAndTime
	if err != nil {
		// 自己判断链码返回的是否为空
		return txi, fmt.Errorf(err.Error())
	}
	if result == nil {
		return txi, fmt.Errorf("user %s dose not exist.", username)
	}

	for _, value := range result {
		if value.IsDelete == false {
			txi = TxidAndTime{
				Number:    username,
				Txid:      value.TxId,
				Timestamp: value.Timestamp,
			}
		}
	}
	return txi, nil
}

// 返回最近的username
func (s *SmartContract) GetNum(ctx contractapi.TransactionContextInterface) (string, error) {
	result, err := s.QueryAllUser(ctx)
	if err != nil {
		return "", fmt.Errorf(err.Error())
	}
	// 按key 排序 这里用的是冒泡排序 可能会比较慢 但是不一定会排序 因为查询结果本身就是有序的
	for i := 1; i < len(result); i++ {
		if result[i].Key < result[i-1].Key {
			cinfo := QueryResult{
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
		return "100000", nil
	}
	return result[len(result)-1].Key, nil
}
func main() {

	chainCode, err := contractapi.NewChaincode(new(SmartContract))

	if err != nil {
		fmt.Printf("Error create manageuser chainCode: %s", err.Error())
		return
	}

	if err := chainCode.Start(); err != nil {
		fmt.Printf("Error starting manageuser chainCode: %s", err.Error())
	}
}
